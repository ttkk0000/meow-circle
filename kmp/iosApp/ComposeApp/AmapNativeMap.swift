import UIKit
import CoreLocation
import MndMobileUI
@preconcurrency import MAMapKit
@preconcurrency import AMapFoundationKit

/// 仅负责SDK渲染；业务与粗点处理由KMP Store负责。
@MainActor
final class AmapNativeMapFactory: NSObject, @preconcurrency IosMapFactory {
    var configured: Bool {
        !(Bundle.main.object(forInfoDictionaryKey: "MND_AMAP_IOS_KEY") as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    func create(onEvent: @escaping (IosMapEvent) -> KotlinUnit) -> any IosMapSurface {
        MAMapView.updatePrivacyShow(.didShow, privacyInfo: .didContain)
        MAMapView.updatePrivacyAgree(.didAgree)
        AMapServices.shared().apiKey = Bundle.main.object(forInfoDictionaryKey: "MND_AMAP_IOS_KEY") as? String
        AMapServices.shared().enableHTTPS = true
        return AmapNativeMapSurface(onEvent: onEvent)
    }
}

@MainActor
private final class LostAnnotation: MAPointAnnotation {
    let recordID: String
    init(point: LostMapPoint, coordinate: CLLocationCoordinate2D) {
        recordID = point.id
        super.init()
        self.coordinate = coordinate
        title = point.title
        subtitle = "公开大致区域"
    }
}

@MainActor
private final class AmapNativeMapSurface: NSObject, @preconcurrency IosMapSurface, @preconcurrency MAMapViewDelegate {
    private let container = UIView()
    private var map: MAMapView?
    private var onEvent: ((IosMapEvent) -> KotlinUnit)?
    private var pickable = false
    private var rendered = ""
    private var focused = ""
    private var stroke = UIColor.label
    var view: UIView { container }

    init(onEvent: @escaping (IosMapEvent) -> KotlinUnit) {
        self.onEvent = onEvent
        super.init()
        let native: MAMapView = MAMapView(frame: .zero)
        map = native
        native.translatesAutoresizingMaskIntoConstraints = false
        native.delegate = self
        native.showsUserLocation = false
        native.showsScale = true
        container.addSubview(native)
        NSLayoutConstraint.activate([
            native.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            native.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            native.topAnchor.constraint(equalTo: container.topAnchor),
            native.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
    }

    func render(points: [LostMapPoint], selectedId: String, center: LostMapPoint, pickable: Bool, tint: Int32) {
        guard let map else { return }
        self.pickable = pickable
        let color = UInt32(bitPattern: tint)
        stroke = UIColor(red: CGFloat((color >> 16) & 255) / 255, green: CGFloat((color >> 8) & 255) / 255,
                         blue: CGFloat(color & 255) / 255, alpha: 1)
        let signature = points.map { "\($0.id)|\($0.title)|\($0.latitude)|\($0.longitude)|\($0.coordinateSystem)" }.joined(separator: ";") + "|\(selectedId)|\(pickable)|\(tint)"
        if signature != rendered {
            map.removeAnnotations(map.annotations)
            map.removeOverlays(map.overlays)
            for point in points {
                guard let position = coordinate(point) else { continue }
                map.addAnnotation(LostAnnotation(point: point, coordinate: position))
                if point.id == selectedId || pickable { map.add(MACircle(center: position, radius: 1000)) }
            }
            rendered = signature
        }
        let focus = points.first { $0.id == selectedId } ?? center
        let focusKey = "\(focus.id)|\(focus.latitude)|\(focus.longitude)|\(focus.coordinateSystem)"
        if focused != focusKey, let position = coordinate(focus) {
            map.setZoomLevel(focus.id == "country-center" ? 4 : 12, animated: false)
            map.setCenter(position, animated: false)
            focused = focusKey
        }
    }

    private func coordinate(_ point: LostMapPoint) -> CLLocationCoordinate2D? {
        let raw = CLLocationCoordinate2D(latitude: point.latitude, longitude: point.longitude)
        guard CLLocationCoordinate2DIsValid(raw) else { return nil }
        switch point.coordinateSystem {
        case "gcj02": return raw
        case "wgs84": return AMapCoordinateConvert(raw, .GPS)
        default: return nil
        }
    }

    func mapView(_ mapView: MAMapView!, didSingleTappedAt coordinate: CLLocationCoordinate2D) {
        guard pickable else { return }
        _ = onEvent?(IosMapEvent(kind: "pick", id: "", latitude: coordinate.latitude, longitude: coordinate.longitude))
    }
    func mapView(_ mapView: MAMapView!, didSelect view: MAAnnotationView!) {
        guard let annotation = view.annotation as? LostAnnotation else { return }
        _ = onEvent?(IosMapEvent(kind: "select", id: annotation.recordID, latitude: 0, longitude: 0))
    }
    func mapView(_ mapView: MAMapView!, rendererFor overlay: MAOverlay!) -> MAOverlayRenderer! {
        guard let circle = overlay as? MACircle else { return nil }
        let renderer: MACircleRenderer = MACircleRenderer(circle: circle)
        renderer.strokeColor = stroke
        renderer.fillColor = stroke.withAlphaComponent(0.12)
        renderer.lineWidth = 2
        return renderer
    }
    func mapViewDidFinishLoadingMap(_ mapView: MAMapView!) {
        _ = onEvent?(IosMapEvent(kind: "loaded", id: "", latitude: 0, longitude: 0))
    }
    func mapViewDidFailLoadingMap(_ mapView: MAMapView!, withError error: Error!) {
        _ = onEvent?(IosMapEvent(kind: "failed", id: "", latitude: 0, longitude: 0))
    }
    func close() {
        map?.delegate = nil
        map?.showsUserLocation = false
        map?.removeFromSuperview()
        map = nil
        onEvent = nil
    }
}
