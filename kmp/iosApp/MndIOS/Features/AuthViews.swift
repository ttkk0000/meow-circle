import SwiftUI

struct LoginView: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme
    @State private var username = ""
    @State private var password = ""

    var body: some View {
        AuthShell(
            eyebrow: "WELCOME BACK",
            title: "回到猫咪的圈子",
            subtitle: "登录 M&D，继续看看今天的猫猫日常。"
        ) {
            VStack(spacing: 14) {
                TextField("用户名", text: $username)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .mndField()
                SecureField("密码", text: $password)
                    .textContentType(.password)
                    .mndField()

                if let message = appStore.state.authMessage {
                    Text(message)
                        .font(.mndBody(.footnote))
                        .foregroundStyle(theme.palette.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button {
                    appStore.send(.login(username: username, password: password))
                } label: {
                    if appStore.state.authBusy {
                        ProgressView().tint(.white)
                    } else {
                        Text("登录")
                    }
                }
                .buttonStyle(MndPrimaryButtonStyle())
                .disabled(appStore.state.authBusy)

                Button("还没有账号？去注册") {
                    appStore.send(.showAuth(.register))
                }
                .font(.mndBody(.subheadline, weight: .semibold))

                if let hint = appStore.state.healthHint, !hint.isEmpty {
                    Text(hint)
                        .font(.mndBody(.caption))
                        .foregroundStyle(theme.palette.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }
}

struct RegisterView: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme
    @State private var username = ""
    @State private var password = ""
    @State private var nickname = ""
    @State private var phone = ""
    @State private var smsCode = ""

    var body: some View {
        AuthShell(
            eyebrow: "JOIN M&D",
            title: "给猫咪一个新身份",
            subtitle: "猫咪优先，也欢迎狗狗伙伴加入。"
        ) {
            VStack(spacing: 12) {
                TextField("用户名", text: $username)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .mndField()
                TextField("昵称（可选）", text: $nickname)
                    .textContentType(.nickname)
                    .mndField()
                SecureField("密码（至少 6 位）", text: $password)
                    .textContentType(.newPassword)
                    .mndField()
                TextField("手机号（可选）", text: $phone)
                    .textContentType(.telephoneNumber)
                    .keyboardType(.phonePad)
                    .mndField()
                TextField("短信验证码（可选）", text: $smsCode)
                    .keyboardType(.numberPad)
                    .mndField()

                if let message = appStore.state.authMessage {
                    Text(message)
                        .font(.mndBody(.footnote))
                        .foregroundStyle(theme.palette.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button {
                    appStore.send(
                        .register(
                            username: username,
                            password: password,
                            nickname: nickname,
                            phone: phone,
                            smsCode: smsCode
                        )
                    )
                } label: {
                    if appStore.state.authBusy {
                        ProgressView().tint(.white)
                    } else {
                        Text("创建账号")
                    }
                }
                .buttonStyle(MndPrimaryButtonStyle())
                .disabled(appStore.state.authBusy)

                Button("返回登录") {
                    appStore.send(.showAuth(.login))
                }
                .font(.mndBody(.subheadline, weight: .semibold))
            }
        }
    }
}

struct ModePickerView: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("选择今天的 M&D")
                        .font(.system(size: 34, weight: .black, design: .rounded))
                    Text("模式只决定内容入口，账号、KMP 会话和主题会继续共享。")
                        .font(.mndBody())
                        .foregroundStyle(theme.palette.textSecondary)
                }

                ForEach(AppMode.allCases) { mode in
                    Button {
                        appStore.send(.selectMode(mode))
                    } label: {
                        MndCard {
                            HStack(spacing: 16) {
                                Image(systemName: mode.symbol)
                                    .font(.system(size: 26, weight: .bold))
                                    .foregroundStyle(theme.palette.brand)
                                    .frame(width: 48, height: 48)
                                    .background(theme.palette.surfaceLow)
                                    .clipShape(Circle())
                                VStack(alignment: .leading, spacing: 5) {
                                    Text(mode.title)
                                        .font(.mndTitle(.title3, weight: .bold))
                                    Text(mode.subtitle)
                                        .font(.mndBody(.subheadline))
                                        .foregroundStyle(theme.palette.textSecondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(theme.palette.textSecondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(20)
        }
    }
}

struct MndThemePicker: View {
    @Environment(MndThemeStore.self) private var theme
    private let columns = [GridItem(.adaptive(minimum: 72), spacing: 8)]

    var body: some View {
        LazyVGrid(columns: columns, alignment: .leading, spacing: 8) {
            ForEach(MndThemeKind.allCases) { kind in
                Button(kind.title) {
                    theme.select(kind)
                }
                .font(.mndBody(.caption, weight: .semibold))
                .padding(.horizontal, 12)
                .frame(maxWidth: .infinity, minHeight: 44)
                .foregroundStyle(kind == theme.selection ? Color.white : theme.palette.textPrimary)
                .background(kind == theme.selection ? theme.palette.brand : theme.palette.surfaceLow)
                .clipShape(Capsule())
                .accessibilityAddTraits(kind == theme.selection ? .isSelected : [])
            }
        }
    }
}

private struct AuthShell<Content: View>: View {
    @Environment(MndThemeStore.self) private var theme
    let eyebrow: String
    let title: String
    let subtitle: String
    private let content: Content

    init(
        eyebrow: String,
        title: String,
        subtitle: String,
        @ViewBuilder content: () -> Content
    ) {
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                MndThemePicker()
                    .frame(maxWidth: .infinity, alignment: .trailing)

                VStack(alignment: .leading, spacing: 8) {
                    Label(eyebrow, systemImage: "pawprint.fill")
                        .font(.mndBody(.caption, weight: .bold))
                        .foregroundStyle(theme.palette.brand)
                    Text(title)
                        .font(.system(size: 36, weight: .black, design: .rounded))
                    Text(subtitle)
                        .font(.mndBody(.title3))
                        .foregroundStyle(theme.palette.textSecondary)
                }

                MndCard { content }
            }
            .padding(20)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
    }
}

private extension View {
    func mndField() -> some View {
        modifier(MndFieldModifier())
    }
}

private struct MndFieldModifier: ViewModifier {
    @Environment(MndThemeStore.self) private var theme

    func body(content: Content) -> some View {
        content
            .font(.mndBody())
            .padding(.horizontal, 14)
            .frame(minHeight: 50)
            .background(theme.palette.surfaceLow)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(theme.palette.outlineVariant, lineWidth: 1)
            }
    }
}
