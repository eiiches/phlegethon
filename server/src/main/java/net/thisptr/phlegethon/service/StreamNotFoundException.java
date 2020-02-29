package net.thisptr.phlegethon.service;

import net.thisptr.phlegethon.model.StreamId;

public class StreamNotFoundException extends RuntimeException {

    public StreamNotFoundException(String namespace, StreamId streamId) {
        super("Stream (namespace = " + namespace + ", stream = " + streamId + ") does not exist.");
    }
}
