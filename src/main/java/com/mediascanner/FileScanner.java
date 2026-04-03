package com.mediascanner;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileScanner {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "ico", "svg", "heic", "heif", "raw", "cr2", "nef", "arw"
    ));

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ts", "mts", "m2ts", "vob", "rm", "rmvb"
    ));

    public interface ScanProgressListener {
        void onProgress(String currentPath, int found);
        void onComplete(List<MediaFile> files);
        void onError(String message);
    }

    public void scanDirectory(String rootPath, ScanProgressListener listener) {
        List<MediaFile> results = new ArrayList<>();

        try {
            Path startPath = Paths.get(rootPath);
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    String ext = getExtension(fileName);

                    if (IMAGE_EXTENSIONS.contains(ext)) {
                        results.add(new MediaFile(
                            fileName,
                            file.toAbsolutePath().toString(),
                            attrs.size(),
                            MediaFile.MediaType.IMAGE,
                            ext
                        ));
                        listener.onProgress(file.toString(), results.size());

                    } else if (VIDEO_EXTENSIONS.contains(ext)) {
                        results.add(new MediaFile(
                            fileName,
                            file.toAbsolutePath().toString(),
                            attrs.size(),
                            MediaFile.MediaType.VIDEO,
                            ext
                        ));
                        listener.onProgress(file.toString(), results.size());
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Skip files we can't access (permission denied, etc.)
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip system/hidden directories on Windows
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (dirName.startsWith("$") || dirName.equals("System Volume Information")
                            || dirName.equals("Windows") || dirName.equals("WinSxS")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            listener.onComplete(results);

        } catch (IOException e) {
            listener.onError("Scan failed: " + e.getMessage());
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    public static List<String> getAvailableDrives() {
        List<String> drives = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            drives.add(root.toString());
        }
        return drives;
    }
}