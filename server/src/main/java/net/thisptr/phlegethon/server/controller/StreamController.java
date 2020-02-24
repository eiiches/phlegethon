package net.thisptr.phlegethon.server.controller;

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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/namespaces/" + NamespaceController.NAMESPACE_PATTERN + "/streams")
public class StreamController {
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

    @RequestMapping(method = RequestMethod.GET , path = "/{streamId:[a-z0-9]+}")
    public Stream get(@PathVariable(value = "namespace") String namespace,
                      @PathVariable(value = "streamId") StreamId streamId) throws Exception {
        return recordingService.getStream(namespace, streamId);
    }
}
