import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            ComposeView()
                .ignoresSafeArea()
            // The app switcher's picture must not show a hidden habit name. UNVERIFIED: build and run on a Mac.
            if scenePhase != .active && PrivacyCover.shared.coversAppSwitcher {
                Color(red: 29 / 255, green: 26 / 255, blue: 57 / 255)
                    .ignoresSafeArea()
            }
        }
    }
}