package net.thisptr.phlegethon.blob;

import net.thisptr.phlegethon.blob.types.jfr.JfrBlobHandler;

import java.util.HashMap;
import java.util.Map;

public class BlobTypeRegistry {
    private final Map<String, BlobTypeRegistration> types = new HashMap<>();

    private static final BlobTypeRegistry INSTANCE = new BlobTypeRegistry();

    public static BlobTypeRegistry getInstance() {
        return INSTANCE;
    }

    private BlobTypeRegistry() {
    }

    public void register(int id, String name, BlobHandler handler) {
        types.put(name, new BlobTypeRegistration(id, name, handler));
    }

    // For now, just manually & statically register here.
    static {
        INSTANCE.register(1, "jfr", new JfrBlobHandler());
    }

    public BlobTypeRegistration getRegistration(String type) throws UnsupportedBlobTypeException {
        BlobTypeRegistration registration = types.get(type);
        if (registration == null)
            throw new UnsupportedBlobTypeException(type);
        return registration;
    }
}
