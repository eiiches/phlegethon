package net.thisptr.phlegethon.server.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.thisptr.phlegethon.misc.Pair;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.Stream;
import net.thisptr.phlegethon.model.StreamId;
import net.thisptr.phlegethon.service.RecordingService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/namespaces/" + NamespaceController.NAMESPACE_PATTERN + "/streams")
public class StreamController {
    private static final String STREAM_ID_PATTERN = "{streamId:[a-z0-9]+}";

    private final RecordingService recordingService;

    @Autowired
    public StreamController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/search")
    public List<Stream> search(@PathVariable(value = "namespace", required = true) String namespace,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "start", required = true) long start,
                               @RequestParam(value = "end", required = true) long end,
                               @RequestParam Map<String, String> allParams) throws Exception {
        Map<String, String> labels = Utils.extractParamsByPrefix(allParams, "label.");
        return recordingService.search(namespace, type, labels, Pair.of(new DateTime(start), new DateTime(end)));
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + STREAM_ID_PATTERN)
    public Stream get(@PathVariable(value = "namespace") String namespace,
                      @PathVariable(value = "streamId") StreamId streamId) throws Exception {
        return recordingService.getStream(namespace, streamId);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordingList {

        @JsonProperty("recordings")
        public List<Recording> recordings;

        @JsonProperty("next_cursor")
        public String cursor;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + STREAM_ID_PATTERN + "/recordings")
    public RecordingList get(@PathVariable(value = "namespace") String namespace,
                             @PathVariable(value = "streamId") StreamId streamId,
                             @RequestParam(value = "cursor", required = false) String cursor) throws Exception {
        return new RecordingList();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/download")
    public void download(@PathVariable(value = "namespace", required = true) String namespace,
                         @PathVariable("path") String path,
                         OutputStream os) {
        recordingService.download(namespace, path, os);
    }
}
