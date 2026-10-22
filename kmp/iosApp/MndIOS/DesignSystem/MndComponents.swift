import SwiftUI

struct MndCard<Content: View>: View {
    @Environment(MndThemeStore.self) private var theme
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.palette.surface)
            .clipShape(RoundedRectangle(cornerRadius: theme.consumerCardRadius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: theme.consumerCardRadius, style: .continuous)
                    .stroke(theme.palette.outlineVariant, lineWidth: 1)
            }
    }
}

struct MndPrimaryButtonStyle: ButtonStyle {
    @Environment(MndThemeStore.self) private var theme

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.mndBody(.headline, weight: .semibold))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(configuration.isPressed ? theme.palette.brandStrong : theme.palette.brand)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
    }
}

struct PawLoadingView: View {
    @Environment(MndThemeStore.self) private var theme
    @State private var rotating = false
    var label = "Loading..."
    var size: CGFloat = 40

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .stroke(theme.palette.surfaceLow, lineWidth: 4)
                Circle()
                    .trim(from: 0.08, to: 0.72)
                    .stroke(theme.palette.brand, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(rotating ? 360 : 0))
                Image(systemName: "pawprint.fill")
                    .font(.system(size: size * 0.35, weight: .semibold))
                    .foregroundStyle(theme.palette.brand)
            }
            .frame(width: size, height: size)

            Text(label)
                .font(.mndBody(.caption, weight: .medium))
                .foregroundStyle(theme.palette.textSecondary)
        }
        .onAppear {
            withAnimation(.linear(duration: 1).repeatForever(autoreverses: false)) {
                rotating = true
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
    }
}

struct MndEmptyState: View {
    @Environment(MndThemeStore.self) private var theme
    let icon: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(theme.palette.brand)
            Text(title)
                .font(.mndTitle(.headline, weight: .bold))
            Text(message)
                .font(.mndBody())
                .foregroundStyle(theme.palette.textSecondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.mndBody(.subheadline, weight: .semibold))
                    .buttonStyle(.bordered)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
    }
}

struct MndToast: View {
    @Environment(MndThemeStore.self) private var theme
    let message: String

    var body: some View {
        Text(message)
            .font(.mndBody(.subheadline, weight: .semibold))
            .foregroundStyle(theme.palette.textPrimary)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(theme.palette.surfaceContainerHigh)
            .clipShape(Capsule())
            .overlay { Capsule().stroke(theme.palette.outlineVariant, lineWidth: 1) }
            .shadow(color: .black.opacity(0.12), radius: 12, y: 4)
            .padding(.top, 8)
    }
}
