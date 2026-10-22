import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case feed
    case market
    case messages
    case orders
    case profile

    var id: String { rawValue }

    var title: String {
        switch self {
        case .feed: "Feed"
        case .market: "Market"
        case .messages: "Messages"
        case .orders: "Orders"
        case .profile: "Profile"
        }
    }

    var symbol: String {
        switch self {
        case .feed: "house.fill"
        case .market: "bag.fill"
        case .messages: "bubble.left.and.bubble.right.fill"
        case .orders: "list.clipboard.fill"
        case .profile: "person.crop.circle.fill"
        }
    }
}

enum AppRoute: Hashable {
    case post(id: Int64)
    case listing(id: Int64)
    case conversation(peerID: Int64)
    case order(id: Int64)
}

struct MainTabView: View {
    @Environment(MndThemeStore.self) private var theme
    @State private var selection: AppTab = .feed
    @State private var feedPath = NavigationPath()
    @State private var marketPath = NavigationPath()
    @State private var messagesPath = NavigationPath()
    @State private var ordersPath = NavigationPath()
    @State private var profilePath = NavigationPath()

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack(path: $feedPath) {
                FeedScreen()
                    .withMndDestinations()
            }
            .tabItem { Label(AppTab.feed.title, systemImage: AppTab.feed.symbol) }
            .tag(AppTab.feed)

            NavigationStack(path: $marketPath) {
                MarketScreen()
                    .withMndDestinations()
            }
            .tabItem { Label(AppTab.market.title, systemImage: AppTab.market.symbol) }
            .tag(AppTab.market)

            NavigationStack(path: $messagesPath) {
                MessagesScreen()
                    .withMndDestinations()
            }
            .tabItem { Label(AppTab.messages.title, systemImage: AppTab.messages.symbol) }
            .tag(AppTab.messages)

            NavigationStack(path: $ordersPath) {
                OrdersScreen()
                    .withMndDestinations()
            }
            .tabItem { Label(AppTab.orders.title, systemImage: AppTab.orders.symbol) }
            .tag(AppTab.orders)

            NavigationStack(path: $profilePath) {
                ProfileScreen()
                    .withMndDestinations()
            }
            .tabItem { Label(AppTab.profile.title, systemImage: AppTab.profile.symbol) }
            .tag(AppTab.profile)
        }
        .tint(theme.palette.brand)
    }
}

private extension View {
    func withMndDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            RouteScaffoldView(route: route)
        }
    }
}

private struct RouteScaffoldView: View {
    @Environment(MndThemeStore.self) private var theme
    let route: AppRoute

    private var title: String {
        switch route {
        case .post: "动态详情"
        case .listing: "商品详情"
        case .conversation: "对话"
        case .order: "订单详情"
        }
    }

    private var identifier: String {
        switch route {
        case .post(let id), .listing(let id), .order(let id): "#\(id)"
        case .conversation(let peerID): "用户 #\(peerID)"
        }
    }

    var body: some View {
        MndCard {
            VStack(alignment: .leading, spacing: 10) {
                Text(title)
                    .font(.mndTitle())
                Text(identifier)
                    .font(.mndBody(.headline, weight: .semibold))
                    .foregroundStyle(theme.palette.brand)
                Text("路由与独立 Tab 历史已经就绪；详情业务 Store 可以按同一 State/Action 模式接入。")
                    .font(.mndBody())
                    .foregroundStyle(theme.palette.textSecondary)
            }
        }
        .padding(16)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .mndScreenBackground()
    }
}
