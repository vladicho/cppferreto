import Foundation

class M3U8Downloader {

    var playlistURL: URL
    var segments: [URL] = []

    init(playlistURL: URL) {
        self.playlistURL = playlistURL
    }

    func downloadPlaylist(completion: @escaping (Error?) -> Void) {
        URLSession.shared.dataTask(with: playlistURL) { data, response, error in
            guard let data = data, error == nil else {
                completion(error)
                return
            }
            self.parsePlaylist(data: data)
            completion(nil)
        }.resume()
    }

    func parsePlaylist(data: Data) {
        if let content = String(data: data, encoding: .utf8) {
            let lines = content.split(separator: '\n')
            for line in lines {
                if line.hasSuffix(".ts") {
                    if let segmentURL = URL(string: String(line)) {
                        segments.append(segmentURL)
                    }
                }
            }
        }
    }

    func downloadSegments(completion: @escaping (Error?) -> Void) {
        let group = DispatchGroup()

        for segment in segments {
            group.enter()
            URLSession.shared.downloadTask(with: segment) { location, response, error in
                if let location = location {
                    // Handle file operations (e.g., saving the file)
                    let fileURL = self.getDocumentsDirectory().appendingPathComponent(segment.lastPathComponent)
                    try? FileManager.default.moveItem(at: location, to: fileURL)
                }
                group.leave()
            }.resume()
        }

        group.notify(queue: .main) {
            completion(nil)
        }
    }

    func getDocumentsDirectory() -> URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        return paths[0]
    }
}

// Usage example
let downloader = M3U8Downloader(playlistURL: URL(string: "https://example.com/playlist.m3u8")!)
downloader.downloadPlaylist { error in
    if error == nil {
        downloader.downloadSegments { error in
            if let error = error {
                print("Error downloading segments: \(error)")
            } else {
                print("All segments downloaded successfully.")
            }
        }
    } else {
        print("Error downloading playlist: \(error)")
    }
}