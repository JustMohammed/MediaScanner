package com.mediascanner;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class FileScanner {

    public interface ScanProgressListener {
        void onProgress(String currentPath, int found);
        void onComplete(List<MediaFile> files);
        void onError(String message);
    }

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm"));

    public void scanDirectory(String path, ScanProgressListener listener) {
        List<MediaFile> foundFiles = new ArrayList<>();
        File root = new File(path);

        if (!root.exists() || !root.isDirectory()) {
            listener.onError("Invalid directory: " + path);
            return;
        }

        try {
            Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    // Skip system and hidden folders
                    if (name.startsWith(".") || name.equalsIgnoreCase("Windows") || name.equalsIgnoreCase("System Volume Information") || name.equalsIgnoreCase("$Recycle.Bin")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                    File file = filePath.toFile();
                    String name = file.getName();
                    String extension = getFileExtension(name).toLowerCase();

                    MediaFile.MediaType type = null;
                    if (IMAGE_EXTENSIONS.contains(extension)) {
                        type = MediaFile.MediaType.IMAGE;
                    } else if (VIDEO_EXTENSIONS.contains(extension)) {
                        type = MediaFile.MediaType.VIDEO;
                    }

                    if (type != null) {
                        try {
                            // Extract creation date for the 6th argument
                            String creationDate = attrs.creationTime().toString().substring(0, 10);

                            // Create MediaFile with all 6 required arguments
                            MediaFile media = new MediaFile(
                                name,
                                file.getAbsolutePath(),
                                file.length(),
                                type,
                                extension,
                                creationDate
                            );
                            foundFiles.add(media);
                            listener.onProgress(file.getAbsolutePath(), foundFiles.size());
                        } catch (Exception e) {
                            // Skip files with unreadable attributes
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, java.io.IOException exc) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });

            listener.onComplete(foundFiles);

        } catch (Exception e) {
            listener.onError("Scan failed: " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex > 0 && lastIndex < fileName.length() - 1) {
            return fileName.substring(lastIndex + 1);
        }
        return "";
    }

    public static List<String> getAvailableDrives() {
        List<String> drives = new ArrayList<>();
        for (File drive : File.listRoots()) {
            drives.add(drive.getAbsolutePath());
        }
        return drives;
    }
}