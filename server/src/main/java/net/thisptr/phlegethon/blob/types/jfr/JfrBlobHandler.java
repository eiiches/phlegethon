package net.thisptr.phlegethon.blob.types.jfr;

import com.google.errorprone.annotations.Var;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.thisptr.phlegethon.blob.BlobHandler;
import net.thisptr.phlegethon.misc.Pair;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Path;

public class JfrBlobHandler implements BlobHandler {

    @Override
    public Pair<DateTime, DateTime> analyzeTimeRange(Path path) throws IOException {
        @Var long minTime = Long.MAX_VALUE;
        @Var long maxTime = Long.MIN_VALUE;
        try (RecordingFile recordingFile = new RecordingFile(path)) {
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();
                if (event.getStartTime().toEpochMilli() < minTime)
                    minTime = event.getStartTime().toEpochMilli();
                if (maxTime < event.getEndTime().toEpochMilli())
                    maxTime = event.getEndTime().toEpochMilli();
            }
        }
        return Pair.of(new DateTime(minTime), new DateTime(maxTime));
    }
}
