import Observation
import shared
import SwiftUI

enum LoadState<Value> {
    case idle
    case loading
    case loaded(Value)
    case empty
    case failed(String)
}

@MainActor
@Observable
final class LoadStore<Value> {
    private(set) var state: LoadState<Value> = .idle

    @ObservationIgnored private var request: IosRequestHandle?
    @ObservationIgnored private var revision = 0

    func load(
        isEmpty: @escaping (Value) -> Bool = { _ in false },
        operation: (@escaping (Value?, String?) -> KotlinUnit) -> IosRequestHandle
    ) {
        revision += 1
        let activeRevision = revision
        request?.cancel()
        state = .loading
        request = operation { [weak self] value, message in
            Task { @MainActor in
                guard let self, self.revision == activeRevision else { return }
                self.request = nil
                if let message {
                    self.state = .failed(message)
                } else if let value {
                    self.state = isEmpty(value) ? .empty : .loaded(value)
                } else {
                    self.state = .failed("没有收到有效数据")
                }
            }
            return KotlinUnit()
        }
    }

    func cancel() {
        revision += 1
        request?.cancel()
        request = nil
    }
}

struct LoadStateContent<Value, LoadedContent: View>: View {
    let state: LoadState<Value>
    let emptyTitle: String
    let emptyMessage: String
    let retry: () -> Void
    private let loadedContent: (Value) -> LoadedContent

    init(
        state: LoadState<Value>,
        emptyTitle: String,
        emptyMessage: String,
        retry: @escaping () -> Void,
        @ViewBuilder loadedContent: @escaping (Value) -> LoadedContent
    ) {
        self.state = state
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.retry = retry
        self.loadedContent = loadedContent
    }

    var body: some View {
        switch state {
        case .idle, .loading:
            PawLoadingView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .empty:
            MndEmptyState(
                icon: "pawprint",
                title: emptyTitle,
                message: emptyMessage,
                actionTitle: "刷新",
                action: retry
            )
        case .failed(let message):
            MndEmptyState(
                icon: "wifi.exclamationmark",
                title: "暂时无法加载",
                message: message,
                actionTitle: "重试",
                action: retry
            )
        case .loaded(let value):
            loadedContent(value)
        }
    }
}
