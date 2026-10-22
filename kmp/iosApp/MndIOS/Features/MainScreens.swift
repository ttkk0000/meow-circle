import Foundation
import shared
import SwiftUI

struct FeedScreen: View {
    @Environment(AppStore.self) private var appStore
    @State private var store = LoadStore<[PostFeedItem]>()
    @State private var composerPresented = false

    var body: some View {
        LoadStateContent(
            state: store.state,
            emptyTitle: "圈子还很安静",
            emptyMessage: "第一条猫咪日常，也许就从你开始。",
            retry: load
        ) { items in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(items, id: \.post.id) { item in
                        NavigationLink(value: AppRoute.post(id: item.post.id)) {
                            FeedCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
        }
        .navigationTitle(appStore.state.mode?.title ?? "M&D")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    composerPresented = true
                } label: {
                    Image(systemName: "plus.circle.fill")
                }
                .accessibilityLabel("发布动态")
            }
        }
        .sheet(isPresented: $composerPresented) {
            ComposerScaffoldView()
        }
        .task { load() }
        .onDisappear { store.cancel() }
        .mndScreenBackground()
    }

    private func load() {
        store.load(isEmpty: \.isEmpty) { completion in
            appStore.bridge.loadFeed(filter: "rec", completion: completion)
        }
    }
}

private struct FeedCard: View {
    @Environment(MndThemeStore.self) private var theme
    let item: PostFeedItem

    var body: some View {
        MndCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 10) {
                    Image(systemName: "cat.fill")
                        .foregroundStyle(theme.palette.brand)
                        .frame(width: 36, height: 36)
                        .background(theme.palette.surfaceLow)
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.author.nickname.isEmpty ? item.author.username : item.author.nickname)
                            .font(.mndBody(.subheadline, weight: .semibold))
                        Text(item.post.createdAt)
                            .font(.mndBody(.caption))
                            .foregroundStyle(theme.palette.textSecondary)
                            .lineLimit(1)
                    }
                }

                if !item.post.title.isEmpty {
                    Text(item.post.title)
                        .font(.mndTitle(.title3, weight: .bold))
                }
                Text(item.post.content)
                    .font(.mndBody())
                    .foregroundStyle(theme.palette.textSecondary)
                    .lineLimit(5)

                Label("\(item.likeCount)", systemImage: item.liked ? "heart.fill" : "heart")
                    .font(.mndBody(.caption, weight: .semibold))
                    .foregroundStyle(item.liked ? theme.palette.brand : theme.palette.textSecondary)
            }
        }
    }
}

struct MarketScreen: View {
    @Environment(AppStore.self) private var appStore
    @State private var store = LoadStore<[Listing]>()

    var body: some View {
        LoadStateContent(
            state: store.state,
            emptyTitle: "还没有闲置发布",
            emptyMessage: "这里适合可信的出售、交换、求购与赠送。",
            retry: load
        ) { listings in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(listings, id: \.id) { listing in
                        NavigationLink(value: AppRoute.listing(id: listing.id)) {
                            ListingCard(listing: listing)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
        }
        .navigationTitle("Market")
        .task { load() }
        .onDisappear { store.cancel() }
        .mndScreenBackground()
    }

    private func load() {
        store.load(isEmpty: \.isEmpty) { completion in
            appStore.bridge.loadListings(completion: completion)
        }
    }
}

private struct ListingCard: View {
    @Environment(MndThemeStore.self) private var theme
    let listing: Listing

    var body: some View {
        MndCard {
            VStack(alignment: .leading, spacing: 10) {
                Label(listing.category.isEmpty ? listing.type : listing.category, systemImage: "tag.fill")
                    .font(.mndBody(.caption, weight: .semibold))
                    .foregroundStyle(theme.palette.brand)
                Text(listing.title)
                    .font(.mndTitle(.title3, weight: .bold))
                Text("¥\(String(format: "%.2f", Double(listing.priceCents) / 100))")
                    .font(.system(.title2, design: .rounded, weight: .black))
                    .foregroundStyle(theme.palette.brandStrong)
            }
        }
    }
}

struct MessagesScreen: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme
    @State private var store = LoadStore<[Conversation]>()

    var body: some View {
        LoadStateContent(
            state: store.state,
            emptyTitle: "暂无消息",
            emptyMessage: "聊猫咪、领养或交易时，对话会出现在这里。",
            retry: load
        ) { conversations in
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(conversations, id: \.peer.id) { conversation in
                        NavigationLink(value: AppRoute.conversation(peerID: conversation.peer.id)) {
                            MndCard {
                                HStack(spacing: 12) {
                                    Image(systemName: "person.crop.circle.fill")
                                        .font(.system(size: 38))
                                        .foregroundStyle(theme.palette.brand)
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(conversation.peer.nickname.isEmpty ? conversation.peer.username : conversation.peer.nickname)
                                            .font(.mndBody(.headline, weight: .semibold))
                                        Text(conversation.lastMessage)
                                            .font(.mndBody(.subheadline))
                                            .foregroundStyle(theme.palette.textSecondary)
                                            .lineLimit(1)
                                    }
                                    Spacer()
                                    if conversation.unreadCount > 0 {
                                        Text("\(conversation.unreadCount)")
                                            .font(.mndBody(.caption, weight: .bold))
                                            .foregroundStyle(Color.white)
                                            .padding(7)
                                            .background(theme.palette.brand)
                                            .clipShape(Circle())
                                    }
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
        }
        .navigationTitle("Messages")
        .task { load() }
        .onDisappear { store.cancel() }
        .mndScreenBackground()
    }

    private func load() {
        store.load(isEmpty: \.isEmpty) { completion in
            appStore.bridge.loadConversations(completion: completion)
        }
    }
}

struct OrdersScreen: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme
    @State private var store = LoadStore<[Order]>()

    var body: some View {
        LoadStateContent(
            state: store.state,
            emptyTitle: "暂无订单",
            emptyMessage: "达成交易后，订单状态会在这里持续更新。",
            retry: load
        ) { orders in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(orders, id: \.id) { order in
                        NavigationLink(value: AppRoute.order(id: order.id)) {
                            MndCard {
                                VStack(alignment: .leading, spacing: 9) {
                                    HStack {
                                        Text(order.listingTitle)
                                            .font(.mndBody(.headline, weight: .semibold))
                                        Spacer()
                                        Text(order.status)
                                            .font(.mndBody(.caption, weight: .bold))
                                            .foregroundStyle(theme.palette.brand)
                                    }
                                    Text("订单 #\(order.id)")
                                        .font(.mndBody(.caption))
                                        .foregroundStyle(theme.palette.textSecondary)
                                    Text("¥\(String(format: "%.2f", Double(order.amountCents) / 100))")
                                        .font(.system(.title3, design: .rounded, weight: .black))
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
        }
        .navigationTitle("Orders")
        .task { load() }
        .onDisappear { store.cancel() }
        .mndScreenBackground()
    }

    private func load() {
        store.load(isEmpty: \.isEmpty) { completion in
            appStore.bridge.loadOrders(role: "buyer", completion: completion)
        }
    }
}

struct ProfileScreen: View {
    @Environment(AppStore.self) private var appStore
    @Environment(MndThemeStore.self) private var theme

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                MndCard {
                    HStack(spacing: 14) {
                        Image(systemName: "cat.circle.fill")
                            .font(.system(size: 58))
                            .foregroundStyle(theme.palette.brand)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(displayName)
                                .font(.mndTitle())
                            Text("@\(appStore.state.user?.username ?? "meow")")
                                .font(.mndBody(.subheadline))
                                .foregroundStyle(theme.palette.textSecondary)
                        }
                    }
                }

                MndCard {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("主题")
                            .font(.mndTitle(.headline, weight: .bold))
                        MndThemePicker()
                    }
                }

                MndCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("当前模式")
                            .font(.mndTitle(.headline, weight: .bold))
                        Text(appStore.state.mode?.title ?? "未选择")
                            .font(.mndBody())
                            .foregroundStyle(theme.palette.textSecondary)
                        Button("切换模式") { appStore.send(.changeMode) }
                            .buttonStyle(.bordered)
                    }
                }

                Button("退出登录", role: .destructive) {
                    appStore.send(.logout)
                }
                .buttonStyle(.bordered)
            }
            .padding(16)
        }
        .navigationTitle("Profile")
        .mndScreenBackground()
    }

    private var displayName: String {
        guard let user = appStore.state.user else { return "M&D 用户" }
        return user.nickname.isEmpty ? user.username : user.nickname
    }
}

private struct ComposerScaffoldView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(MndThemeStore.self) private var theme
    @State private var text = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("发布 Store 可按 AppAction / Effect 模式接入 KMP `createPost`。")
                    .font(.mndBody(.subheadline))
                    .foregroundStyle(theme.palette.textSecondary)
                TextEditor(text: $text)
                    .padding(10)
                    .background(theme.palette.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(theme.palette.outlineVariant, lineWidth: 1)
                    }
                Spacer()
            }
            .padding(16)
            .navigationTitle("写一条猫咪日常")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
            .mndScreenBackground()
        }
    }
}
