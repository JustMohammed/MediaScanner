package com.mediascanner;

public class MediaFile {
    public enum MediaType { IMAGE, VIDEO }

    private String name;
    private String path;
    private long sizeInBytes;
    private MediaType mediaType;
    private String extension;

    public MediaFile(String name, String path, long size, MediaType type, String ext) {
        this.name = name;
        this.path = path;
        this.sizeInBytes = size;
        this.mediaType = type;
        this.extension = ext;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getExtension() { return extension; }
    public MediaType getMediaType() { return mediaType; }
    public String getType() { return mediaType.toString(); }
    public String getSize() { return (sizeInBytes / 1024) + " KB"; }
}