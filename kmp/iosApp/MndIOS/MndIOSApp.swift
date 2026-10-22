import Foundation
import shared
import SwiftUI

@main
struct MndIOSApp: App {
    @State private var appStore: AppStore
    @State private var themeStore: MndThemeStore

    init() {
        let configuredBaseURL = Bundle.main.object(forInfoDictionaryKey: "MND_API_BASE_URL") as? String
        let baseURL = configuredBaseURL?.trimmingCharacters(in: .whitespacesAndNewlines)
        let bridge = IosSdkBridge(
            defaultBaseUrl: (baseURL?.isEmpty == false ? baseURL : nil) ?? "http://127.0.0.1:8080"
        )
        _appStore = State(initialValue: AppStore(bridge: bridge))
        _themeStore = State(
            initialValue: MndThemeStore(storedValue: bridge.currentTheme()) { kind in
                bridge.setTheme(value: kind.rawValue)
            }
        )
    }

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environment(appStore)
                .environment(themeStore)
                .preferredColorScheme(themeStore.preferredColorScheme)
        }
    }
}

struct AppRootView: View {
    @Environment(AppStore.self) private var appStore

    var body: some View {
        Group {
            switch appStore.state.phase {
            case .restoring:
                SplashView()
            case .signedOut:
                switch appStore.state.authDestination {
                case .login: LoginView()
                case .register: RegisterView()
                }
            case .choosingMode:
                ModePickerView()
            case .signedIn:
                MainTabView()
            }
        }
        .mndScreenBackground()
        .overlay(alignment: .top) {
            if case .message(let message) = appStore.effect {
                MndToast(message: message)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.snappy, value: appStore.effectRevision)
        .task {
            appStore.send(.bootstrap)
        }
        .onChange(of: appStore.effectRevision) { _, revision in
            guard revision > 0 else { return }
            Task { @MainActor in
                try? await Task.sleep(for: .seconds(2))
                if appStore.effectRevision == revision {
                    appStore.consumeEffect()
                }
            }
        }
    }
}

private struct SplashView: View {
    @Environment(MndThemeStore.self) private var theme

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "pawprint.fill")
                .font(.system(size: 58, weight: .black))
                .foregroundStyle(theme.palette.brand)
            Text("M&D")
                .font(.system(size: 44, weight: .black, design: .rounded))
            Text("meow & doggie")
                .font(.mndBody(.headline, weight: .medium))
                .foregroundStyle(theme.palette.textSecondary)
            PawLoadingView(label: "正在找回你和猫咪的圈子", size: 40)
                .padding(.top, 16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
