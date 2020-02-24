package net.thisptr.phlegethon.service;

import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.StreamId;

public class RecordingNotFoundException extends RuntimeException {
    public RecordingNotFoundException(String namespace, StreamId streamId, RecordingFileName recordingName) {
        super(String.format("Recording (namespace = %s, stream = %s, name = %s) does not exist.", namespace, streamId, recordingName));
    }
}
