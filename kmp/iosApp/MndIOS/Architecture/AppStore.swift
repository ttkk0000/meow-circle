import Foundation
import Observation
import shared

enum AppMode: String, CaseIterable, Identifiable {
    case community = "Community"
    case adoption = "Adoption"
    case trade = "Trade"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .community: "猫猫社区"
        case .adoption: "领养与救助"
        case .trade: "闲置与服务"
        }
    }

    var subtitle: String {
        switch self {
        case .community: "分享猫咪日常，也欢迎狗狗伙伴"
        case .adoption: "寻找家、发布领养与救助信息"
        case .trade: "可信的宠物闲置、服务与交换"
        }
    }

    var symbol: String {
        switch self {
        case .community: "pawprint.fill"
        case .adoption: "heart.fill"
        case .trade: "arrow.left.arrow.right"
        }
    }
}

enum AuthDestination {
    case login
    case register
}

enum AppPhase {
    case restoring
    case signedOut
    case choosingMode
    case signedIn
}

struct AppState {
    var phase: AppPhase = .restoring
    var user: User?
    var mode: AppMode?
    var authDestination: AuthDestination = .login
    var authBusy = false
    var authMessage: String?
    var healthHint: String?
}

enum AppAction {
    case bootstrap
    case bootstrapCompleted(IosBootstrapSnapshot)
    case showAuth(AuthDestination)
    case login(username: String, password: String)
    case loginCompleted(user: User?, message: String?)
    case register(username: String, password: String, nickname: String, phone: String, smsCode: String)
    case registerCompleted(user: User?, message: String?)
    case selectMode(AppMode)
    case changeMode
    case logout
}

enum AppEffect: Equatable {
    case message(String)
}

@MainActor
@Observable
final class AppStore {
    let bridge: IosSdkBridge
    private(set) var state: AppState
    private(set) var effect: AppEffect?
    private(set) var effectRevision = 0

    @ObservationIgnored private var request: IosRequestHandle?
    @ObservationIgnored private var didBootstrap = false

    init(bridge: IosSdkBridge) {
        self.bridge = bridge
        state = AppState(
            user: bridge.cachedUser(),
            mode: bridge.currentAppMode().flatMap(AppMode.init(rawValue:))
        )
    }

    func send(_ action: AppAction) {
        switch action {
        case .bootstrap:
            guard !didBootstrap else { return }
            didBootstrap = true
            state.phase = .restoring
            replaceRequest(
                bridge.bootstrap { [weak self] snapshot in
                    Task { @MainActor in
                        self?.send(.bootstrapCompleted(snapshot))
                    }
                    return KotlinUnit()
                }
            )

        case .bootstrapCompleted(let snapshot):
            request = nil
            state.user = snapshot.user
            state.healthHint = snapshot.healthHint
            resolveSignedInPhase()

        case .showAuth(let destination):
            guard !state.authBusy else { return }
            state.authDestination = destination
            state.authMessage = nil

        case .login(let username, let password):
            let normalizedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !normalizedUsername.isEmpty, !password.isEmpty else {
                state.authMessage = "请输入用户名和密码"
                return
            }
            state.authBusy = true
            state.authMessage = nil
            replaceRequest(
                bridge.login(username: normalizedUsername, password: password) { [weak self] user, message in
                    Task { @MainActor in
                        self?.send(.loginCompleted(user: user, message: message))
                    }
                    return KotlinUnit()
                }
            )

        case .loginCompleted(let user, let message):
            finishAuthentication(user: user, message: message)

        case .register(let username, let password, let nickname, let phone, let smsCode):
            let normalizedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !normalizedUsername.isEmpty, password.count >= 6 else {
                state.authMessage = "请输入用户名，密码至少 6 位"
                return
            }
            state.authBusy = true
            state.authMessage = nil
            replaceRequest(
                bridge.register(
                    username: normalizedUsername,
                    password: password,
                    nickname: nickname,
                    phone: phone,
                    smsCode: smsCode
                ) { [weak self] user, message in
                    Task { @MainActor in
                        self?.send(.registerCompleted(user: user, message: message))
                    }
                    return KotlinUnit()
                }
            )

        case .registerCompleted(let user, let message):
            finishAuthentication(user: user, message: message)

        case .selectMode(let mode):
            bridge.setAppMode(value: mode.rawValue)
            state.mode = mode
            state.phase = .signedIn

        case .changeMode:
            state.phase = .choosingMode

        case .logout:
            request?.cancel()
            request = nil
            bridge.logout()
            state.user = nil
            state.authBusy = false
            state.authMessage = nil
            state.authDestination = .login
            state.phase = .signedOut
            emit(.message("已退出登录"))
        }
    }

    func consumeEffect() {
        effect = nil
    }

    private func finishAuthentication(user: User?, message: String?) {
        request = nil
        state.authBusy = false
        guard let user else {
            state.authMessage = message ?? "请求失败，请稍后重试"
            return
        }
        state.user = user
        state.authMessage = nil
        state.authDestination = .login
        resolveSignedInPhase()
    }

    private func resolveSignedInPhase() {
        guard state.user != nil else {
            state.phase = .signedOut
            return
        }
        state.phase = state.mode == nil ? .choosingMode : .signedIn
    }

    private func replaceRequest(_ next: IosRequestHandle) {
        request?.cancel()
        request = next
    }

    private func emit(_ next: AppEffect) {
        effect = next
        effectRevision += 1
    }
}
