import AuthenticationServices
import Foundation
import UIKit
import MndMobileUI

/** 只处理官方SDK与系统授权回调，不持有应用会话、业务Store或关联决定。 */
@MainActor
final class NativeProviderFactory: NSObject, @preconcurrency IosProviderFactory, @preconcurrency WXApiDelegate {
    private struct Pending {
        let provider: String
        let state: String
        var callback: ((IosProviderResult) -> KotlinUnit)?
    }
    private var pending: Pending?
    private var qq: TencentOAuth?
    private var qqDelegate: QqLoginDelegate?
    private var apple: AppleLoginSession?

    private func setting(_ key: String) -> String { (Bundle.main.object(forInfoDictionaryKey: key) as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines) }
    private func link(_ key: String) -> String? {
        let value = setting(key)
        guard let url = URL(string: value), url.scheme == "https", let host = url.host, host != "example.invalid", url.user == nil, url.password == nil else { return nil }
        return value
    }
    func configured(provider: String) -> Bool {
        switch provider {
        case "wechat": return setting("MND_WECHAT_IOS_APP_ID").range(of: "^wx[a-zA-Z0-9]{16}$", options: .regularExpression) != nil && setting("MND_WECHAT_IOS_APP_ID") != "wx0000000000000000" && link("MND_WECHAT_UNIVERSAL_LINK") != nil
        case "qq": return setting("MND_QQ_IOS_APP_ID").range(of: "^[1-9][0-9]{4,19}$", options: .regularExpression) != nil && link("MND_QQ_UNIVERSAL_LINK") != nil
        case "apple": return ["YES", "TRUE", "1"].contains(setting("MND_APPLE_SIGNIN_ENABLED").uppercased())
        default: return false
        }
    }
    func start(provider: String, state: String, nonce: String, controller: UIViewController, onResult: @escaping (IosProviderResult) -> KotlinUnit) {
        guard pending == nil else { _ = onResult(IosProviderResult(kind: "pending", state: state, code: "", identityToken: "")); return }
        guard configured(provider: provider), let window = controller.viewIfLoaded?.window else {
            _ = onResult(IosProviderResult(kind: "failed", state: state, code: "", identityToken: "")); return
        }
        pending = Pending(provider: provider, state: state, callback: onResult)
        switch provider {
        case "wechat":
            guard MndWechatRegister(setting("MND_WECHAT_IOS_APP_ID"), link("MND_WECHAT_UNIVERSAL_LINK")!) else { finish(state: state, kind: "failed"); return }
            MndWechatAuthorize(controller, state, self) { [weak self] accepted in
                Task { @MainActor in if !accepted { self?.finish(state: state, kind: "failed") } }
            }
        case "qq":
            let delegate = QqLoginDelegate(state: state, owner: self)
            qqDelegate = delegate
            let sdk = MndQqSetup(setting("MND_QQ_IOS_APP_ID"), link("MND_QQ_UNIVERSAL_LINK")!, delegate)
            qq = sdk
            if !MndQqAuthorize(sdk) { finish(state: state, kind: "failed") }
        case "apple":
            let session = AppleLoginSession(window: window, state: state, nonce: nonce) { [weak self] kind, code, token in
                self?.finish(state: state, kind: kind, code: code, token: token)
            }
            apple = session
            session.start()
        default: finish(state: state, kind: "failed")
        }
    }
    func cancel(state: String) {
        guard pending?.state == state else { return }
        if pending?.provider == "qq" {
            // QQ没有可核实的SSO取消API：丢弃本次回调，等原SDK返回后释放并发槽，避免串到下一账号。
            pending?.callback = nil
        } else {
            pending = nil; apple = nil
        }
    }
    private func finish(state: String, kind: String, code: String = "", token: String = "") {
        guard let original = pending, original.state == state else { return }
        pending = nil
        if let qq, let qqDelegate { MndQqClear(qq, qqDelegate) }
        qq = nil; qqDelegate = nil; apple = nil
        _ = original.callback?(IosProviderResult(kind: kind, state: state, code: code, identityToken: token))
    }
    fileprivate func qqCompleted(state: String, kind: String) {
        guard pending?.state == state, pending?.provider == "qq" else { return }
        let code = kind == "success" ? qq.flatMap { MndQqCode($0) } ?? "" : ""
        finish(state: state, kind: kind == "success" && code.isEmpty ? "failed" : kind, code: code)
    }
    func onResp(_ resp: BaseResp) {
        guard let response = resp as? SendAuthResp, let state = response.state, pending?.provider == "wechat", pending?.state == state else { return }
        if response.errCode == 0, let code = response.code, !code.isEmpty { finish(state: state, kind: "success", code: code) }
        else { finish(state: state, kind: response.errCode == -2 ? "cancelled" : "failed") }
    }
    func handle(url: URL) -> Bool {
        switch pending?.provider {
        case "wechat": return MndWechatHandleURL(url, self)
        case "qq": return MndQqHandleURL(url)
        default: return false
        }
    }
    func handle(activity: NSUserActivity) -> Bool {
        switch pending?.provider {
        case "wechat": return MndWechatHandleUniversal(activity, self)
        case "qq": return activity.webpageURL.map(MndQqHandleUniversal) ?? false
        default: return false
        }
    }
}

@MainActor
private final class QqLoginDelegate: NSObject, @preconcurrency TencentSessionDelegate {
    let state: String
    weak var owner: NativeProviderFactory?
    init(state: String, owner: NativeProviderFactory) { self.state = state; self.owner = owner }
    func tencentDidLogin() { owner?.qqCompleted(state: state, kind: "success") }
    func tencentDidNotLogin(_ cancelled: Bool) { owner?.qqCompleted(state: state, kind: cancelled ? "cancelled" : "failed") }
    func tencentDidNotNetWork() { owner?.qqCompleted(state: state, kind: "failed") }
}

@MainActor
private final class AppleLoginSession: NSObject, @preconcurrency ASAuthorizationControllerDelegate, @preconcurrency ASAuthorizationControllerPresentationContextProviding {
    let window: UIWindow
    let state: String
    let nonce: String
    let completion: (String, String, String) -> Void
    private var controller: ASAuthorizationController?
    init(window: UIWindow, state: String, nonce: String, completion: @escaping (String, String, String) -> Void) {
        self.window = window; self.state = state; self.nonce = nonce; self.completion = completion
    }
    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.state = state
        request.nonce = nonce // 与服务端为本次挑战生成并核验的nonce一致。
        let controller = ASAuthorizationController(authorizationRequests: [request])
        self.controller = controller
        controller.delegate = self; controller.presentationContextProvider = self
        controller.performRequests()
    }
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor { window }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential, credential.state == state,
              let bytes = credential.identityToken, let token = String(data: bytes, encoding: .utf8), !token.isEmpty,
              let authorization = credential.authorizationCode, let code = String(data: authorization, encoding: .utf8), !code.isEmpty else {
            completion("failed", "", ""); return
        }
        // 服务端以一次性code换取并校验ID Token及可撤销的refresh token，不能只交本地JWT。
        completion("success", code, token)
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        completion((error as? ASAuthorizationError)?.code == .canceled ? "cancelled" : "failed", "", "")
    }
}
