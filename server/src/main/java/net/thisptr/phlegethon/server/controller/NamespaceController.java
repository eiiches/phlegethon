package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.Namespace;
import net.thisptr.phlegethon.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/namespaces")
public class NamespaceController {
    public static final String NAMESPACE_PATTERN = "{namespace:[a-z][a-z0-9-]*}";

    private final NamespaceService namespaceService;

    @Autowired
    public NamespaceController(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/" + NAMESPACE_PATTERN)
    public Namespace createNamespace(@PathVariable("namespace") String namespace) {
        return namespaceService.createNamespace(namespace);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/" + NAMESPACE_PATTERN)
    public Namespace deleteNamespace(@PathVariable("namespace") String namespace) {
        return namespaceService.deleteNamespace(namespace);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + NAMESPACE_PATTERN)
    public Namespace getNamespace(@PathVariable("namespace") String namespace) {
        return namespaceService.getNamespace(namespace);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/")
    public List<Namespace> listNamespaces() {
        return namespaceService.listNamespaces();
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/" + NAMESPACE_PATTERN)
    public Namespace updateNamespace(@PathVariable("namespace") String namespace) {
        return namespaceService.updateNamespace(namespace);
    }
}
