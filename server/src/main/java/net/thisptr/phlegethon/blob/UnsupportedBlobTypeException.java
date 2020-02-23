package net.thisptr.phlegethon.blob;

public class UnsupportedBlobTypeException extends RuntimeException {

    public UnsupportedBlobTypeException(String type) {
        super("Blob type (" + type + ") is not supported.");
    }
}
