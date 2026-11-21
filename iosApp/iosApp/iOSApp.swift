import SwiftUI
import UIKit
import Shared

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Routes Home-screen quick actions (Info.plist UIApplicationShortcutItems) through a scene delegate, the only place
/// SwiftUI apps receive them. UNVERIFIED: written on Windows; build and run on a Mac.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        // Cold start from a quick action: the item comes with the connection options.
        if let item = options.shortcutItem {
            IosShortcuts.shared.handle(type: item.type)
        }
        let config = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        config.delegateClass = ShortcutSceneDelegate.self
        return config
    }
}

final class ShortcutSceneDelegate: NSObject, UIWindowSceneDelegate {
    // App already running: iOS calls this instead.
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        IosShortcuts.shared.handle(type: shortcutItem.type)
        completionHandler(true)
    }
}
