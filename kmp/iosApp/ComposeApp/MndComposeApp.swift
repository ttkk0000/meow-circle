import SwiftUI
import UIKit
import MndMobileUI

@main
struct MndComposeApp: App {
    @UIApplicationDelegateAdaptor(MndPushAppDelegate.self) private var pushDelegate
    @Environment(\.scenePhase) private var scenePhase
    private let client: IosClient
    private let providerFactory: NativeProviderFactory

    init() {
        let providerFactory = NativeProviderFactory()
        self.providerFactory = providerFactory
        IosProviderPlatform.shared.factory = providerFactory
        IosMapPlatform.shared.factory = AmapNativeMapFactory()
        IosPushPlatform.shared.controller = NativeDevicePush.shared
        let value = Bundle.main.object(forInfoDictionaryKey: "MND_API_BASE_URL") as? String
        let baseURL = value?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "http://127.0.0.1:8080"
        client = IosClient(baseUrl: baseURL, secrets: KeychainSecretStorage())
        NativeDevicePush.shared.attach(client: client)
    }

    var body: some Scene {
        WindowGroup {
            ComposeRoot(client: client)
                .ignoresSafeArea() // Compose owns safeDrawing and keyboard insets.
                .preferredColorScheme(.light)
                .onOpenURL { _ = providerFactory.handle(url: $0) }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { _ = providerFactory.handle(activity: $0) }
                .onChange(of: scenePhase) { _, phase in if phase == .active { NativeDevicePush.shared.foreground() } }
        }
    }
}

private struct ComposeRoot: UIViewControllerRepresentable {
    let client: IosClient
    func makeUIViewController(context: Context) -> UIViewController {
        client.makeViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private extension String {
    var nonEmpty: String? { isEmpty ? nil : self }
}
