package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonValue;

public class StreamId {
    private byte[] bytes;

    public StreamId(byte[] bytes) {
        if (bytes.length != 20)
            throw new IllegalArgumentException("StreamId must have 20 bytes");
        this.bytes = bytes.clone();
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public byte[] toBytes() {
        return bytes.clone();
    }

    @JsonValue
    public String toHex() {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[((b & 0xff) >>> 4)]);
            sb.append(HEX_CHARS[b & 0x0f]);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toHex();
    }
}
