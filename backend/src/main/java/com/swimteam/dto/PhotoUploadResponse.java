package com.swimteam.dto;

public class PhotoUploadResponse {

    private boolean hasPhoto;
    private String contentType;
    private int sizeBytes;
    private String message;

    public PhotoUploadResponse(boolean hasPhoto, String contentType, int sizeBytes, String message) {
        this.hasPhoto = hasPhoto;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.message = message;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public String getMessage() {
        return message;
    }
}
