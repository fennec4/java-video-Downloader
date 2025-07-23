import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class VideoDownloader {
    
    // Répertoire de téléchargement par défaut
    private static final String DOWNLOAD_DIR = System.getProperty("user.home") + "/Downloads/VideoDownloader/";
    
    // Chemins vers les binaires locaux (à côté du .jar)
    private static final String LOCAL_BINARIES_DIR = "./bin/";
    private static final String YT_DLP_PATH = getYtDlpPath();
    private static final String FFMPEG_PATH = getFfmpegPath();
    
    // Détection automatique du système d'exploitation
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
    
    // Enum pour les formats supportés
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
            // Vérifier si les binaires locaux sont disponibles
            if (!downloader.areLocalBinariesAvailable()) {
                System.out.println("❌ Binaires non trouvés dans le dossier: " + LOCAL_BINARIES_DIR);
                System.out.println("📁 Structure requise:");
                System.out.println("   📂 bin/");
                System.out.println("      📄 yt-dlp.exe (Windows) ou yt-dlp (Linux/Mac)");
                System.out.println("      📄 ffmpeg.exe (Windows) ou ffmpeg (Linux/Mac)");
                return;
            }
            
            // Créer le répertoire de téléchargement
            downloader.createDownloadDirectory();
            
            System.out.println("=== TÉLÉCHARGEUR DE VIDÉOS (MODE LOCAL) ===");
            System.out.println("✅ yt-dlp: " + YT_DLP_PATH);
            System.out.println("✅ FFmpeg: " + FFMPEG_PATH);
            System.out.println("📁 Téléchargements: " + DOWNLOAD_DIR);
            
            while (true) {
                System.out.println("\n=== TÉLÉCHARGEUR VIDÉO SIMPLE ===");
                System.out.println("1. Télécharger en MP3 (audio haute qualité)");
                System.out.println("2. Télécharger en MP4 (MEILLEURE RÉSOLUTION AUTO)");
                System.out.println("3. Télécharger en MP4 720p (résolution moyenne, rapide)");
                System.out.println("4. Quitter");               
                System.out.print("Votre choix: ");
                
                int choice = scanner.nextInt();
                scanner.nextLine();
                
                if (choice == 4) break;
                
                System.out.print("Entrez l'URL de la vidéo: ");
                String url = scanner.nextLine().trim();
                
                if (url.isEmpty()) {
                    System.out.println("URL invalide!");
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
                        System.out.println("Choix invalide!");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
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
            System.out.println("Démarrage du téléchargement en " + format.name() + "...");
            
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
                    "--ffmpeg-location", FFMPEG_PATH, // Spécifier le chemin FFmpeg
                    url
                );
            } else if (format == Format.MP4_720P) {
                System.out.println("🔍 Téléchargement en qualité 720p optimisée...");
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
                    "--ffmpeg-location", FFMPEG_PATH, // Spécifier le chemin FFmpeg
                    url
                );
            } else {
                System.out.println("🔍 Recherche de la meilleure qualité disponible...");
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
                    "--ffmpeg-location", FFMPEG_PATH, // Spécifier le chemin FFmpeg
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
                System.err.println("Le téléchargement a pris trop de temps et a été interrompu.");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                System.out.println("✅ Téléchargement réussi!");
                return true;
            } else {
                System.err.println("❌ Échec du téléchargement (code: " + exitCode + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement: " + e.getMessage());
            return false;
        }
    }
    
   /**
     * Vérifie si les binaires locaux sont disponibles
     */
    private boolean areLocalBinariesAvailable() {
        File ytDlpFile = new File(YT_DLP_PATH);
        File ffmpegFile = new File(FFMPEG_PATH);
        
        boolean ytDlpExists = ytDlpFile.exists() && ytDlpFile.canExecute();
        boolean ffmpegExists = ffmpegFile.exists() && ffmpegFile.canExecute();
        
        if (!ytDlpExists) {
            System.err.println("❌ yt-dlp non trouvé: " + YT_DLP_PATH);
        }
        if (!ffmpegExists) {
            System.err.println("❌ FFmpeg non trouvé: " + FFMPEG_PATH);
        }
        
        return ytDlpExists && ffmpegExists;
    }
    
    /**
     * Crée le répertoire de téléchargement s'il n'existe pas
     */
    private void createDownloadDirectory() {
        try {
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            if (!Files.exists(downloadPath)) {
                Files.createDirectories(downloadPath);
                System.out.println("Répertoire créé: " + DOWNLOAD_DIR);
            }
        } catch (Exception e) {
            System.err.println("Impossible de créer le répertoire: " + e.getMessage());
        }
    }
}