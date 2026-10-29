import SwiftUI

/// Scaffold Hello — bind colors to generated `GymTokens` (design SSOT).
struct ContentView: View {
    var body: some View {
        ZStack {
            GymTokens.colorBgCanvas.ignoresSafeArea()
            Text("GymManager iOS")
                .foregroundStyle(GymTokens.colorTextPrimary)
                .padding(GymTokens.space4)
        }
    }
}

#Preview {
    ContentView()
}
