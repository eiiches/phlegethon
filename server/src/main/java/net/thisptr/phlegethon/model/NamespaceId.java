package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonValue;

public class NamespaceId {
    private int id;

    public NamespaceId(int id) {
        this.id = id;
    }

    @JsonValue
    public int toInt() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
