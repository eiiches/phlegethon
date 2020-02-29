package net.thisptr.phlegethon.blob;

import net.thisptr.phlegethon.misc.Pair;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BlobHandler {

    Pair<DateTime, DateTime> analyzeTimeRange(Path path) throws IOException;

    /**
     * Encodes blob data into a format that is more appropriate for long-term storage. The implementation, for example, can
     * perform compression of original data for storage space efficiency. Users must call {@link OutputStream#close()} on the
     * returned stream when the stream is no longer needed, which in turn closes the OutputStream passed as an argument.
     *
     * @param os
     * @return
     */
    OutputStream encode(OutputStream os) throws IOException;

    /**
     * Decodes storage data into its original format.
     *
     * @param is
     * @return
     * @see #encode(OutputStream)
     */
    InputStream decode(InputStream is) throws IOException;
}
