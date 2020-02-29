package net.thisptr.phlegethon.blob.types.jfr;

import com.github.luben.zstd.ZstdOutputStream;
import com.google.errorprone.annotations.Var;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.thisptr.phlegethon.blob.BlobHandler;
import net.thisptr.phlegethon.misc.Pair;
import org.joda.time.DateTime;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.IllegalFormatException;

import com.github.luben.zstd.ZstdInputStream;

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

    @Override
    public OutputStream encode(OutputStream os) throws IOException {
        return new ZstdOutputStream(os, 19);
    }

    public boolean isZstd(byte[] magic) {
        // See https://tools.ietf.org/html/rfc8478#section-6.1
        return magic[0] == 0x28
                && magic[1] == (byte) 0xb5
                && magic[2] == 0x2f
                && magic[3] == (byte) 0xfd;
    }

    public boolean isJfr(byte[] magic) {
        // 46 4c 52 (FLR)
        return magic[0] == 0x46
                && magic[1] == 0x4c
                && magic[2] == 0x52;
    }

    @Override
    public InputStream decode(InputStream is) throws IOException {
        byte[] magic = new byte[4];
        BufferedInputStream bis = new BufferedInputStream(is);
        bis.mark(4);
        bis.read(magic);
        bis.reset();

        if (isZstd(magic)) {
            return new ZstdInputStream(bis);
        } else if (isJfr(magic)) {
            return bis;
        } else {
            throw new RuntimeException("Invalid magic. Cannot decode JFR data.");
        }
    }
}
