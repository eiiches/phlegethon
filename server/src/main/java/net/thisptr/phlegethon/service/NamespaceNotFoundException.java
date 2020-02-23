package net.thisptr.phlegethon.service;

public class NamespaceNotFoundException extends RuntimeException {

    public NamespaceNotFoundException(String name) {
        super("Namespace (name = " + name + ") does not exist.");
    }
}
