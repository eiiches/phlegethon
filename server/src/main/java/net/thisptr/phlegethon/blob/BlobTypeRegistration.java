package net.thisptr.phlegethon.blob;

public class BlobTypeRegistration {
    public final int id;
    public final String name;
    public final BlobHandler handler;

    public BlobTypeRegistration(int id, String name, BlobHandler handler) {
        this.id = id;
        this.name = name;
        this.handler = handler;
    }
}
