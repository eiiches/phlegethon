package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonValue;
import net.thisptr.phlegethon.misc.MoreBytes;

public class StreamId {
    private byte[] bytes;

    public StreamId(byte[] bytes) {
        if (bytes.length != 20)
            throw new IllegalArgumentException("StreamId must have 20 bytes");
        this.bytes = bytes.clone();
    }

    public static StreamId valueOf(String s) {
        if (s.length() != 40)
            throw new IllegalArgumentException("StreamId must have 20 bytes (or 40 bytes if hex encoded).");
        return new StreamId(MoreBytes.fromHex(s));
    }

    public byte[] toBytes() {
        return bytes.clone();
    }

    @JsonValue
    public String toHex() {
        return MoreBytes.toHex(bytes);
    }

    @Override
    public String toString() {
        return toHex();
    }
}
