import UIKit
import UserNotifications
import MndMobileUI

/** 原生APNs只接设备令牌与通用提醒；不把推送正文当成业务操作或已读凭据。 */
@MainActor
final class NativeDevicePush: NSObject, @preconcurrency IosPushController, @preconcurrency UNUserNotificationCenterDelegate {
    static let shared = NativeDevicePush()
    private var client: IosClient?
    private var token = ""
    private var enabled = false
    private var registering = false
    private var configured: Bool { Bundle.main.object(forInfoDictionaryKey: "MND_APNS_ENABLED") as? String == "YES" }

    func attach(client: IosClient) {
        self.client = client
        UNUserNotificationCenter.current().delegate = self
        refresh()
    }
    func setEnabled(enabled: Bool) {
        self.enabled = enabled
        if !enabled { UNUserNotificationCenter.current().removeAllDeliveredNotifications() }
        refresh()
    }
    func refresh() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let status = settings.authorizationStatus
            let granted = status == .authorized || status == .provisional || status == .ephemeral
            Task { @MainActor in
                self.client?.updatePushChannel(token: self.token, permission: granted ? "granted" : "denied", available: self.configured)
                if self.enabled && granted && self.configured && !self.registering {
                    self.registering = true
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
    }
    func foreground() { refresh(); client?.refreshPush() }
    func registered(_ value: Data) {
        registering = false
        let next = value.map { String(format: "%02x", $0) }.joined()
        if next != token { token = next; refresh() }
    }
    func failed() { registering = false; client?.pushFailed() }
    func setBadge(count: Int32) { UNUserNotificationCenter.current().setBadgeCount(Int(count)) { _ in } }

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        await MainActor.run {
            self.client?.refreshPush()
            return self.enabled ? [.banner, .sound] : []
        }
    }
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        await MainActor.run { self.foreground() }
    }
}

@MainActor
final class MndPushAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        NativeDevicePush.shared.registered(deviceToken)
    }
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NativeDevicePush.shared.failed()
    }
}
