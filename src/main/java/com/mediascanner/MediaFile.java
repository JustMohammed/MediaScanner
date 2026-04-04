package com.mediascanner;

public class MediaFile {
    public enum MediaType { IMAGE, VIDEO }

    private String name;
    private String path;
    private long sizeInBytes;
    private MediaType mediaType;
    private String extension;
    private String creationDate; // Added

    public MediaFile(String name, String path, long size, MediaType type, String ext, String creationDate) {
        this.name = name;
        this.path = path;
        this.sizeInBytes = size;
        this.mediaType = type;
        this.extension = ext;
        this.creationDate = creationDate; // Added
    }

    public double getEstimatedMinutes() {
        if (mediaType != MediaType.VIDEO) return 0;
        return (double) sizeInBytes / (1024 * 1024 * 80);
    }

    public String getDurationDisplay() {
        if (mediaType != MediaType.VIDEO) return "-";
        
        double totalSeconds = getEstimatedMinutes() * 60;
        
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getExtension() { return extension; }
    public MediaType getMediaType() { return mediaType; }
    public String getType() { return mediaType.toString(); }
    public String getSize() { return (sizeInBytes / 1024) + " KB"; }
    public String getCreationDate() { return creationDate; } // Added
}