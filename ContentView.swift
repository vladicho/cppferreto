import SwiftUI
import WebKit

struct ContentView: View {
    @State private var url = ""
    @State private var username = ""
    @State private var password = ""
    @State private var statusText = ""
    @State private var progress = 0.0
    @State private var webView: WKWebView!

    var body: some View {
        VStack {
            TextField("URL", text: $url)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            TextField("Username", text: $username)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            SecureField("Password", text: $password)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            HStack {
                Button("Open Site") {
                    if let url = URL(string: self.url) {
                        let request = URLRequest(url: url)
                        self.webView.load(request)
                        self.statusText = "Loading..."
                    }
                }
                Button("Download") {
                    // Download Logic
                    self.statusText = "Downloading..."
                }
                Button("Play Video") {
                    // Video Playback Logic
                    self.statusText = "Playing Video..."
                }
            }
            ProgressBar(value: $progress)
                .frame(height: 20)
                .padding()
            Text(statusText)
                .padding()
            WebViewContainer(webView: $webView)
                .frame(height: 400)
        }
        .onAppear { self.webView = WKWebView() }
    }
}

struct WebViewContainer: UIViewRepresentable {
    @Binding var webView: WKWebView?

    func makeUIView(context: Context) -> WKWebView {
        return webView ?? WKWebView()
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        // Update logic if needed
    }
}

struct ProgressBar: View {
    @Binding var value: Double

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Rectangle()
                    .frame(width: geometry.size.width, height: geometry.size.height)
                    .foregroundColor(Color.gray)
                Rectangle()
                    .frame(width: min(CGFloat(self.value) * geometry.size.width, geometry.size.width), height: geometry.size.height)
                    .foregroundColor(Color.blue)
            }
            .cornerRadius(45.0)
        }
    }
}