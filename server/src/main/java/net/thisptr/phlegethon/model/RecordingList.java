package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.thisptr.phlegethon.model.Recording;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordingList {

    @JsonProperty("recordings")
    public List<Recording> recordings;

    @JsonProperty("next_cursor")
    public String cursor;
}
