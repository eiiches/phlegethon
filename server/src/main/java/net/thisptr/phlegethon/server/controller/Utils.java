package net.thisptr.phlegethon.server.controller;

import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static Map<String, String> extractParamsByPrefix(Map<String, String> allParams, String prefix) {
        return allParams.entrySet().stream()
                .filter((entry) -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap((entry) -> entry.getKey().substring(prefix.length()), (entry) -> entry.getValue()));
    }
}
