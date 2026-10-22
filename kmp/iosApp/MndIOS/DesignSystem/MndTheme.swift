import Foundation
import Observation
import SwiftUI

enum MndThemeKind: String, CaseIterable, Identifiable {
    case honey
    case mint
    case night
    case neutral

    var id: String { rawValue }

    var title: String {
        switch self {
        case .honey: "Honey"
        case .mint: "Mint"
        case .night: "Night"
        case .neutral: "Neutral"
        }
    }

    init(storedValue: String) {
        switch storedValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "mint": self = .mint
        case "night": self = .night
        case "neutral", "system": self = .neutral
        case "honey", "sugar": self = .honey
        default: self = .honey
        }
    }
}

struct MndPalette {
    let canvas: Color
    let surface: Color
    let surfaceLow: Color
    let surfaceContainer: Color
    let surfaceContainerHigh: Color
    let textPrimary: Color
    let textSecondary: Color
    let brand: Color
    let brandStrong: Color
    let accent: Color
    let outline: Color
    let outlineVariant: Color
    let error: Color

    static let honey = MndPalette(
        canvas: Color(hex: 0xFFF8F2),
        surface: .white,
        surfaceLow: Color(hex: 0xFFF1E6),
        surfaceContainer: Color(hex: 0xFFF8F2),
        surfaceContainerHigh: .white,
        textPrimary: Color(hex: 0x231F20),
        textSecondary: Color(hex: 0x6B5E57),
        brand: Color(hex: 0xFF8A3D),
        brandStrong: Color(hex: 0x9A4600),
        accent: Color(hex: 0xFFD166),
        outline: Color(hex: 0xCBA88F),
        outlineVariant: Color(hex: 0xF1D8C8),
        error: Color(hex: 0xEF4444)
    )

    static let mint = MndPalette(
        canvas: Color(hex: 0xF5FFFC),
        surface: .white,
        surfaceLow: Color(hex: 0xEAFBF6),
        surfaceContainer: Color(hex: 0xCDEFE6),
        surfaceContainerHigh: .white,
        textPrimary: Color(hex: 0x12312B),
        textSecondary: Color(hex: 0x4D6866),
        brand: Color(hex: 0x2EC4A6),
        brandStrong: Color(hex: 0x12312B),
        accent: Color(hex: 0xA7F3D0),
        outline: Color(hex: 0x8CCFC0),
        outlineVariant: Color(hex: 0xCDEFE6),
        error: Color(hex: 0xEF4444)
    )

    static let night = MndPalette(
        canvas: Color(hex: 0x0B0D12),
        surface: Color(hex: 0x151821),
        surfaceLow: Color(hex: 0x1F2430),
        surfaceContainer: Color(hex: 0x252B38),
        surfaceContainerHigh: Color(hex: 0x151821),
        textPrimary: Color(hex: 0xF8FAFC),
        textSecondary: Color(hex: 0xCBD5E1),
        brand: Color(hex: 0x8B5CF6),
        brandStrong: Color(hex: 0xA78BFA),
        accent: Color(hex: 0xFBBF24),
        outline: Color(hex: 0x475569),
        outlineVariant: Color(hex: 0x303644),
        error: Color(hex: 0xF87171)
    )

    static let neutral = MndPalette(
        canvas: Color(hex: 0xF7F7F8),
        surface: .white,
        surfaceLow: Color(hex: 0xF3F4F6),
        surfaceContainer: Color(hex: 0xE5E7EB),
        surfaceContainerHigh: .white,
        textPrimary: Color(hex: 0x111827),
        textSecondary: Color(hex: 0x4B5563),
        brand: Color(hex: 0x4B5563),
        brandStrong: Color(hex: 0x374151),
        accent: Color(hex: 0x9CA3AF),
        outline: Color(hex: 0x9CA3AF),
        outlineVariant: Color(hex: 0xE5E7EB),
        error: Color(hex: 0xDC2626)
    )
}

@MainActor
@Observable
final class MndThemeStore {
    private(set) var selection: MndThemeKind
    @ObservationIgnored private let persist: (MndThemeKind) -> Void

    init(
        storedValue: String,
        persist: @escaping (MndThemeKind) -> Void = { _ in }
    ) {
        selection = MndThemeKind(storedValue: storedValue)
        self.persist = persist
    }

    var palette: MndPalette {
        switch selection {
        case .honey: .honey
        case .mint: .mint
        case .night: .night
        case .neutral: .neutral
        }
    }

    var preferredColorScheme: ColorScheme {
        selection == .night ? .dark : .light
    }

    var consumerCardRadius: CGFloat { selection == .neutral ? 8 : 16 }

    func select(_ kind: MndThemeKind) {
        guard kind != selection else { return }
        selection = kind
        persist(kind)
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

extension Font {
    static func mndTitle(_ style: TextStyle = .title2, weight: Weight = .bold) -> Font {
        .system(style, design: .rounded, weight: weight)
    }

    static func mndBody(_ style: TextStyle = .body, weight: Weight = .regular) -> Font {
        .system(style, design: .default, weight: weight)
    }
}

extension View {
    func mndScreenBackground() -> some View {
        modifier(MndScreenBackgroundModifier())
    }
}

private struct MndScreenBackgroundModifier: ViewModifier {
    @Environment(MndThemeStore.self) private var theme

    func body(content: Content) -> some View {
        content
            .foregroundStyle(theme.palette.textPrimary)
            .background(theme.palette.canvas.ignoresSafeArea())
            .tint(theme.palette.brand)
    }
}
