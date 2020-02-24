package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown =  true)
public class Stream {
    @JsonProperty("id")
    public StreamId id;

    @JsonProperty("type")
    public String type;

    @JsonProperty("labels")
    public Map<String, String> labels;

    @JsonProperty("first_event_at")
    public DateTime firstEventAt;

    @JsonProperty("last_event_at")
    public DateTime lastEventAt;
}
