package net.thisptr.phlegethon.blob;

import net.thisptr.phlegethon.misc.Pair;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Path;

public interface BlobHandler {

    Pair<DateTime, DateTime> analyzeTimeRange(Path path) throws IOException;
}
