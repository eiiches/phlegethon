package net.thisptr.phlegethon.service;

public class RecordingTooLargeException extends RuntimeException {
    public RecordingTooLargeException(String message) {
        super(message);
    }
}
