import Foundation
import Combine

class VideoDownloaderViewModel: ObservableObject {
    @Published var downloadProgress: Double = 0.0
    @Published var isDownloading: Bool = false
    @Published var error: String?
    
    private var cancellables = Set<AnyCancellable>()
    
    func downloadVideo(from url: URL, to destination: URL) {
        isDownloading = true
        error = nil
        
        let task = URLSession.shared.downloadTask(with: url) { [weak self] location, response, error in
            guard let self = self else { return }
            self.isDownloading = false
            
            if let error = error {
                self.error = error.localizedDescription
                return
            }
            
            guard let location = location else {
                self.error = "Failed to download video."
                return
            }
            
            do {
                try FileManager.default.moveItem(at: location, to: destination)
                self.downloadProgress = 1.0
            } catch {
                self.error = error.localizedDescription
            }
        }
        
        task.resume()
        
        // Track download progress
        let progressObserver = NotificationCenter.default.publisher(for: NSURLSessionTask.didSendBodyDataNotification, object: task)
        progressObserver
            .compactMap { $0.userInfo?[NSKeyedArchiveName] as? Progress }
            .sink { [weak self] progress in
                self?.downloadProgress = progress.fractionCompleted
            }
            .store(in: &cancellables)
    }
}