import SwiftUI
import UIKit
import MndMobileUI

@main
struct MndComposeApp: App {
    private let client: IosClient

    init() {
        let value = Bundle.main.object(forInfoDictionaryKey: "MND_API_BASE_URL") as? String
        let baseURL = value?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "http://127.0.0.1:8080"
        client = IosClient(baseUrl: baseURL, secrets: KeychainSecretStorage())
    }

    var body: some Scene {
        WindowGroup {
            ComposeRoot(client: client)
                .ignoresSafeArea() // Compose owns safeDrawing and keyboard insets.
                .preferredColorScheme(.light)
        }
    }
}

private struct ComposeRoot: UIViewControllerRepresentable {
    let client: IosClient
    func makeUIViewController(context: Context) -> UIViewController {
        client.makeViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private extension String {
    var nonEmpty: String? { isEmpty ? nil : self }
}
