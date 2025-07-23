import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class VideoDownloader {
    
    // Default download directory
    private static final String DOWNLOAD_DIR = System.getProperty("user.home") + "/Downloads/VideoDownloader/";
    
    // Paths to local binaries
    private static final String LOCAL_BINARIES_DIR = "./bin/";
    private static final String YT_DLP_PATH = getYtDlpPath();
    private static final String FFMPEG_PATH = getFfmpegPath();
    
    // Automatic detection of operating system
    private static String getYtDlpPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return LOCAL_BINARIES_DIR + "yt-dlp.exe";
        } else {
            return LOCAL_BINARIES_DIR + "yt-dlp";
        }
    }
    
    private static String getFfmpegPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return LOCAL_BINARIES_DIR + "ffmpeg.exe";
        } else {
            return LOCAL_BINARIES_DIR + "ffmpeg";
        }
    }
    
    // Enum for supported formats
    public enum Format {
        MP3("bestaudio[ext=m4a]/bestaudio/best[ext=m4a]", "mp3"),
        MP4_BEST("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best[ext=mp4]/best", "mp4"),
        MP4_720P("bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=720]+bestaudio[ext=m4a]/best[height<=720][ext=mp4]/best[height<=720]", "mp4");
        
        private final String ytDlpFormat;
        private final String extension;
        
        Format(String ytDlpFormat, String extension) {
            this.ytDlpFormat = ytDlpFormat;
            this.extension = extension;
        }
        
        public String getYtDlpFormat() { return ytDlpFormat; }
        public String getExtension() { return extension; }
    }
    
    public static void main(String[] args) {
        VideoDownloader downloader = new VideoDownloader();
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Check if local binaries are available
            if (!downloader.areLocalBinariesAvailable()) {
                System.out.println("Binaries not found in folder: " + LOCAL_BINARIES_DIR);
                System.out.println("Required structure:");
                System.out.println("    bin/");
                System.out.println("       yt-dlp.exe (Windows) or yt-dlp (Linux/Mac)");
                System.out.println("       ffmpeg.exe (Windows) or ffmpeg (Linux/Mac)");
                return;
            }
            
            // Create the download directory
            downloader.createDownloadDirectory();
           
            while (true) {
                System.out.println("\n=== SIMPLE VIDEO DOWNLOADER ===");               
                System.out.println(" Downloads: " + DOWNLOAD_DIR+"\n");
                System.out.println("1. Download MP3");
                System.out.println("2. Download MP4 (BEST RESOLUTION)");
                System.out.println("3. Download MP4 (720p)");
                System.out.println("4. Quit");               
                System.out.print("Your choice: ");
                
                int choice = scanner.nextInt();
                scanner.nextLine();
                
                if (choice == 4) break;
                
                System.out.print("Enter the video URL: ");
                String url = scanner.nextLine().trim();
                
                if (url.isEmpty()) {
                    System.out.println("Invalid URL!");
                    continue;
                }
                
                switch (choice) {
                    case 1:
                        downloader.downloadVideo(url, Format.MP3);
                        break;
                    case 2:
                        downloader.downloadVideo(url, Format.MP4_BEST);
                        break;
                    case 3:
                        downloader.downloadVideo(url, Format.MP4_720P);
                        break;                    
                    default:
                        System.out.println("Invalid choice!");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Télécharge une vidéo dans le format spécifié
     */
    public boolean downloadVideo(String url, Format format) {
        try {
            System.out.println("Starting the download in " + format.name() + "...");
            
            ProcessBuilder pb = new ProcessBuilder();
            
            if (format == Format.MP3) {
                pb.command(
                    YT_DLP_PATH,
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "--format", "bestaudio[ext=m4a]/bestaudio[ext=mp3]/bestaudio",
                    "--output", DOWNLOAD_DIR + "%(title)s.%(ext)s",
                    "--no-playlist",
                    "--ffmpeg-location", FFMPEG_PATH, // Specify FFmpeg path
                    url
                );
            } else if (format == Format.MP4_720P) {
                System.out.println("Download in 720p quality...");
                pb.command(
                    YT_DLP_PATH,
                    "--format", format.getYtDlpFormat(),
                    "--output", DOWNLOAD_DIR + "%(title)s.%(ext)s",
                    "--no-playlist",
                    "--merge-output-format", "mp4",
                    "--format-sort", "height:720,fps:30,tbr",
                    "--prefer-free-formats", "false",
                    "--embed-thumbnail",
                    "--add-metadata",
                    "--ffmpeg-location", FFMPEG_PATH, // Specify FFmpeg path
                    url
                );
            } else {
                System.out.println(" Seeking the best quality available...");
                pb.command(
                    YT_DLP_PATH,
                    "--format", format.getYtDlpFormat(),
                    "--output", DOWNLOAD_DIR + "%(title)s.%(ext)s",                   
                    "--no-playlist",
                    "--merge-output-format", "mp4",
                    "--format-sort", "res:desc,fps:desc,tbr:desc",
                    "--prefer-free-formats", "false",
                    "--embed-thumbnail",
                    "--add-metadata",
                    "--ffmpeg-location", FFMPEG_PATH, // Specify FFmpeg path
                    url
                );
            }
            
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                System.err.println("The download took too long and was interrupted.");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0 || exitCode == 1) {
                System.out.println("Download successful!");
                return true;
            } else {
                System.err.println("Download failed (code: " + exitCode + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error while downloading: " + e.getMessage());
            return false;
        }
    }
    
   /**
     * Checks if local binaries are available
     */
    private boolean areLocalBinariesAvailable() {
        File ytDlpFile = new File(YT_DLP_PATH);
        File ffmpegFile = new File(FFMPEG_PATH);
        
        boolean ytDlpExists = ytDlpFile.exists() && ytDlpFile.canExecute();
        boolean ffmpegExists = ffmpegFile.exists() && ffmpegFile.canExecute();
        
        if (!ytDlpExists) {
            System.err.println("yt-dlp not found: " + YT_DLP_PATH);
        }
        if (!ffmpegExists) {
            System.err.println("FFmpeg not found: " + FFMPEG_PATH);
        }
        
        return ytDlpExists && ffmpegExists;
    }
    
    /**
     * Creates the download directory if it does not exist
     */
    private void createDownloadDirectory() {
        try {
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            if (!Files.exists(downloadPath)) {
                Files.createDirectories(downloadPath);
                System.out.println("Directory created: " + DOWNLOAD_DIR);
            }
        } catch (Exception e) {
            System.err.println("Unable to create directory: " + e.getMessage());
        }
    }
}