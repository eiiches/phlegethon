package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.service.RecordingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/v1/namespaces/" + NamespaceController.NAMESPACE_PATTERN + "/recordings")
public class RecordingUploadController {
    private final RecordingService recordingService;

    @Autowired
    public RecordingUploadController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/upload")
    public Recording upload(@PathVariable(value = "namespace", required = true) String namespace,
                            @RequestParam(value = "type", required = true) String type,
                            @RequestParam Map<String, String> allParams,
                            InputStream is) throws Exception {
        Map<String, String> labels = Utils.extractParamsByPrefix(allParams, "label.");
        return recordingService.upload(namespace, type, labels, is);
    }
}
