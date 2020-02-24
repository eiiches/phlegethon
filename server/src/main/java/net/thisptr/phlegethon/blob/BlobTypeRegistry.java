package net.thisptr.phlegethon.blob;

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

    public BlobTypeRegistration getRegistration(String type) throws UnsupportedBlobTypeException {
        BlobTypeRegistration registration = types.get(type);
        if (registration == null)
            throw new UnsupportedBlobTypeException(type);
        return registration;
    }

    public BlobTypeRegistration getRegistration(int id) throws UnsupportedBlobTypeException {
        return types.values().stream()
                .filter(registration -> registration.id == id)
                .findFirst()
                .orElseThrow(() -> new UnsupportedBlobTypeException(String.valueOf(id)));
    }
}
