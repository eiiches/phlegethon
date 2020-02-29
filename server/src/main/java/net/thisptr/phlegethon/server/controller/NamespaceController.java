package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.Namespace;
import net.thisptr.phlegethon.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/v1/namespaces")
public class NamespaceController {
    public static final String NAMESPACE_PATTERN = "{namespace:[a-z][a-z0-9-]*}";

    private final NamespaceService namespaceService;

    @Autowired
    public NamespaceController(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @RequestMapping(method = RequestMethod.POST, path = "")
    public Namespace createNamespace(@RequestBody Namespace namespace) throws Exception {
        return namespaceService.createNamespace(namespace);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/" + NAMESPACE_PATTERN)
    public Namespace deleteNamespace(@PathVariable("namespace") String name) throws Exception {
        return namespaceService.deleteNamespace(name);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + NAMESPACE_PATTERN)
    public Namespace getNamespace(@PathVariable("namespace") String name) throws Exception {
        return namespaceService.getNamespace(name);
    }

    @RequestMapping(method = RequestMethod.GET, path = "")
    public List<Namespace> listNamespaces() throws Exception {
        return namespaceService.listNamespaces();
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/" + NAMESPACE_PATTERN)
    public Namespace updateNamespace(@PathVariable("namespace") String name, @RequestBody Namespace namespace) throws Exception {
        return namespaceService.updateNamespace(name, namespace);
    }
}
