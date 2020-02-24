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

    public static StreamId valueOf(String s) {
        if (s.length() != 40)
            throw new IllegalArgumentException("StreamId must have 20 bytes (or 40 bytes if hex encoded).");
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; ++i) {
            bytes[i] = (byte) Integer.parseInt(s.substring(i << 1, (i << 1) + 2), 16);
        }
        return new StreamId(bytes);
    }

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
