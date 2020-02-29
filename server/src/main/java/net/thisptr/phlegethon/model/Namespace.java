package net.thisptr.phlegethon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Namespace {
    @JsonIgnore // We don't expose this ID to users.
    public NamespaceId id;

    @JsonProperty("name")
    public String name;

    @JsonProperty("config")
    public NamespaceConfig config;
}
