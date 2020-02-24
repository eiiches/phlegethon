package net.thisptr.phlegethon.server.controller;

import net.thisptr.phlegethon.model.RecordingFileName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RecordingFileNameConverter implements Converter<String, RecordingFileName> {

    @Override
    public RecordingFileName convert(String s) {
        return RecordingFileName.valueOf(s);
    }
}
