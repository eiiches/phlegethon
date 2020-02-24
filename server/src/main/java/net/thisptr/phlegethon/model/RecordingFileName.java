package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.primitives.Longs;
import net.thisptr.phlegethon.misc.MoreBytes;

public class RecordingFileName {
    public long firstEventAt;
    public long lastEventAt;

    public RecordingFileName(long firstEventAt, long lastEventAt) {
        this.firstEventAt = firstEventAt;
        this.lastEventAt = lastEventAt;
    }

    @Override
    @JsonValue
    public String toString() {
        return MoreBytes.toHex(Longs.toByteArray(firstEventAt)) + MoreBytes.toHex(Longs.toByteArray(lastEventAt));
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RecordingFileName valueOf(String name) {
        if (name.length() != 32)
            throw new IllegalArgumentException("Invalid RecordingFileName.");
        long firstEventAt = Longs.fromByteArray(MoreBytes.fromHex(name.substring(0, 16)));
        long lastEventAt = Longs.fromByteArray(MoreBytes.fromHex(name.substring(16, 32)));
        return new RecordingFileName(firstEventAt, lastEventAt);
    }
}
