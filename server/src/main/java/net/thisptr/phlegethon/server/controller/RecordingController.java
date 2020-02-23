package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.service.RecordingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/namespaces/" + NamespaceController.NAMESPACE_PATTERN + "/recordings")
public class RecordingController {
    private static final String LABEL_PREFIX = "label.";

    private final RecordingService recordingService;

    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    private static Map<String, String> extractLabels(Map<String, String> allParams) {
        return allParams.entrySet().stream()
                .filter((entry) -> entry.getKey().startsWith(LABEL_PREFIX))
                .collect(Collectors.toMap((entry) -> entry.getKey().substring(LABEL_PREFIX.length()), (entry) -> entry.getValue()));
    }

    @RequestMapping(method = RequestMethod.POST, path = "/upload")
    public Recording upload(@PathVariable(value = "namespace", required = true) String namespace,
                            @RequestParam(value = "type", required = true) String type,
                            @RequestParam Map<String, String> allParams,
                            InputStream is) throws Exception {
        Map<String, String> labels = extractLabels(allParams);
        return recordingService.upload(namespace, type, labels, is);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/search")
    public List<Recording> search(@PathVariable(value = "namespace", required = true) String namespace,
                                  @RequestParam(value = "type", required = true) String type,
                                  @RequestParam Map<String, String> allParams) {
        Map<String, String> labels = extractLabels(allParams);
        return recordingService.search(namespace, type, labels);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/download")
    public void download(@PathVariable(value = "namespace", required = true) String namespace,
                         @PathVariable("path") String path,
                         OutputStream os) {
        recordingService.download(namespace, path, os);
    }
}
