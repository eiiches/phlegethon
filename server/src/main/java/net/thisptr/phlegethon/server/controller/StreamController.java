package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.misc.Pair;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.RecordingList;
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
import javax.servlet.http.HttpServletResponse;

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

    @RequestMapping(method = RequestMethod.GET, path = "/" + STREAM_ID_PATTERN + "/recordings")
    public RecordingList get(@PathVariable(value = "namespace") String namespace,
                             @PathVariable(value = "streamId") StreamId streamId,
                             @RequestParam(value = "cursor", required = false) String cursor) throws Exception {
        return recordingService.listRecordings(namespace, streamId, cursor);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + STREAM_ID_PATTERN + "/recordings/{recordingName}")
    public Recording getRecording(@PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "streamId") StreamId streamId,
                                  @PathVariable(value = "recordingName") String recordingName) throws Exception {
        return recordingService.getRecording(namespace, streamId, recordingName);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/" + STREAM_ID_PATTERN + "/recordings/{recordingName}/download")
    public void download(@PathVariable(value = "namespace", required = true) String namespace,
                         @PathVariable(value = "streamId") StreamId streamId,
                         @PathVariable(value = "recordingName") String recordingName,
                         HttpServletResponse servletResponse) throws Exception {
        Recording recording = recordingService.getRecording(namespace, streamId, recordingName );

        StringBuilder filename = new StringBuilder();
        filename.append(namespace); // [a-z][a-z0-9-]*
        filename.append("-");
        filename.append(streamId.toHex());
        filename.append("-");
        filename.append(recording.name);
        filename.append(".");
        filename.append(recording.type);

        // The filename never contains quotes, etc. It'safe.
        servletResponse.setHeader("Content-Disposition", "attachment; filename=\" + filename + \"");
        recordingService.download(namespace, streamId, recordingName, servletResponse.getOutputStream());
    }
}
