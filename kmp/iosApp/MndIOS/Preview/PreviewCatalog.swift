import shared
import SwiftUI

#Preview("Honey loading and empty") {
    let theme = MndThemeStore(storedValue: "honey")
    VStack(spacing: 32) {
        PawLoadingView()
        MndEmptyState(
            icon: "pawprint",
            title: "圈子还很安静",
            message: "第一条猫咪日常，也许就从你开始。"
        )
    }
    .padding()
    .environment(theme)
    .mndScreenBackground()
}

#Preview("Night login") {
    let bridge = IosSdkBridge(defaultBaseUrl: "http://127.0.0.1:8080")
    let appStore = AppStore(bridge: bridge)
    let theme = MndThemeStore(storedValue: "night")
    LoginView()
        .environment(appStore)
        .environment(theme)
        .preferredColorScheme(.dark)
        .mndScreenBackground()
}
