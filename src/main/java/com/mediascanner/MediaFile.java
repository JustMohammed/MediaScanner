package com.mediascanner;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MediaFile {

    public enum MediaType { IMAGE, VIDEO }

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty path = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty extension = new SimpleStringProperty();
    private final MediaType mediaType;
    private final long sizeBytes;

    public MediaFile(String name, String path, long sizeBytes, MediaType mediaType, String extension) {
        this.name.set(name);
        this.path.set(path);
        this.sizeBytes = sizeBytes;
        this.size.set(formatSize(sizeBytes));
        this.type.set(mediaType == MediaType.IMAGE ? "Image" : "Video");
        this.extension.set(extension.toUpperCase());
        this.mediaType = mediaType;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        else return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public StringProperty nameProperty() { return name; }
    public StringProperty pathProperty() { return path; }
    public StringProperty sizeProperty() { return size; }
    public StringProperty typeProperty() { return type; }
    public StringProperty extensionProperty() { return extension; }

    public String getName() { return name.get(); }
    public String getPath() { return path.get(); }
    public String getSize() { return size.get(); }
    public String getType() { return type.get(); }
    public String getExtension() { return extension.get(); }
    public MediaType getMediaType() { return mediaType; }
    public long getSizeBytes() { return sizeBytes; }
}
