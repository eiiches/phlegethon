package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NamespaceConfig {

    @JsonProperty("retention_seconds")
    public long retentionSeconds = 31536000;
}
