package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.StreamId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StreamIdConverter implements Converter<String, StreamId> {
    @Override
    public StreamId convert(String s) {
        return StreamId.valueOf(s);
    }
}
