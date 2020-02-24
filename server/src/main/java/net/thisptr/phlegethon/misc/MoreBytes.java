package net.thisptr.phlegethon.misc;

public class MoreBytes {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[((b & 0xff) >>> 4)]);
            sb.append(HEX_CHARS[b & 0x0f]);
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex.length() % 2 != 0)
            throw new IllegalArgumentException("Odd number of characters.");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; ++i)
            bytes[i] = (byte) Integer.parseInt(hex.substring(i << 1, (i << 1) + 2), 16);
        return bytes;
    }
}
