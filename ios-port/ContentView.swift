import SwiftUI
import WebKit
import AVKit

struct ContentView: View {
    @StateObject private var viewModel = VideoDownloaderViewModel()
    @State private var showVideoPlayer = false
    
    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header - Input Fields
                VStack(spacing: 12) {
                    TextField("Enter URL", text: $viewModel.url)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .padding(.horizontal)
                    
                    HStack(spacing: 10) {
                        TextField("Username", text: $viewModel.username)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                        
                        SecureField("Password", text: $viewModel.password)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                
                // WebView
                WebViewContainer(webView: $viewModel.webView, viewModel: viewModel)
                    .frame(maxHeight: .infinity)
                
                // Status and Progress
                VStack(spacing: 8) {
                    HStack {
                        Text(viewModel.statusText)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                    .padding(.horizontal)
                    
                    if viewModel.isDownloading {
                        ProgressView(value: viewModel.progress)
                            .padding(.horizontal)
                        
                        HStack {
                            Text("\(Int(viewModel.progress * 100))%")
                                .font(.caption2)
                            Spacer()
                            Text("\(viewModel.currentSegment) / \(viewModel.totalSegments)")
                                .font(.caption2)
                        }
                        .padding(.horizontal)
                        .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                
                // Buttons
                HStack(spacing: 10) {
                    Button(action: { viewModel.openSite() }) {
                        Label("Open Site", systemImage: "globe")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    
                    Button(action: { viewModel.downloadVideo() }) {
                        Label("Download", systemImage: "arrow.down.circle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(viewModel.url.isEmpty || viewModel.isDownloading)
                    
                    Button(action: { viewModel.playVideo() }) {
                        Label("Play", systemImage: "play.circle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(!viewModel.videoExists)
                }
                .padding()
            }
        }
        .sheet(isPresented: $showVideoPlayer) {
            if let videoURL = viewModel.videoFileURL {
                VideoPlayer(player: AVPlayer(url: videoURL))
                    .ignoresSafeArea()
            }
        }
        .onAppear {
            viewModel.setupWebView()
            viewModel.checkVideoExists()
        }
    }
}

struct WebViewContainer: UIViewRepresentable {
    @Binding var webView: WKWebView
    var viewModel: VideoDownloaderViewModel
    
    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.mediaPlaybackRequiresUserAction = false
        
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        self.viewModel.webView = uiView
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(viewModel: viewModel)
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var viewModel: VideoDownloaderViewModel
        
        init(viewModel: VideoDownloaderViewModel) {
            self.viewModel = viewModel
        }
        
        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            viewModel.statusText = "Loading..."
        }
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            viewModel.statusText = "Page loaded"
        }
        
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            viewModel.statusText = "Error: \(error.localizedDescription)"
        }
    }
}

#Preview {
    ContentView()
}