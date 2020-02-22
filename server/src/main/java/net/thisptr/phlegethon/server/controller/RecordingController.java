package net.thisptr.phlegethon.server.controller;

import org.springframework.web.bind.annotation.*;

import javax.print.DocFlavor;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recordings")
public class RecordingController {
    private static final String LABEL_PREFIX = "label.";

    private static Map<String, String> extractLabels(Map<String, String> allParams) {
        return allParams.entrySet().stream()
                .filter((entry) -> entry.getKey().startsWith(LABEL_PREFIX))
                .collect(Collectors.toMap((entry) -> entry.getKey().substring(LABEL_PREFIX.length()), (entry) -> entry.getValue()));
    }

    @RequestMapping(method = RequestMethod.POST, path = "/upload")
    public void upload(@RequestParam(value = "type", required = true) String type,
                       @RequestParam Map<String, String> allParams) {
        final Map<String, String> labels = extractLabels(allParams);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/search")
    public void search(@RequestParam(value = "type", required = true) String type,
                       @RequestParam Map<String, String> allParams) {
        final Map<String, String> labels = extractLabels(allParams);

    }

    @RequestMapping(method = RequestMethod.GET, path = "/{recordingId}")
    public void get(@PathVariable("recordingId") String recordingId) {

    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{recordingId}")
    public void delete(@PathVariable("recordingId") String recordingId) {

    }

    @RequestMapping(method = RequestMethod.GET, path = "/{recordingId}/download")
    public void download(@PathVariable("recordingId") String recordingId) {

    }
}
