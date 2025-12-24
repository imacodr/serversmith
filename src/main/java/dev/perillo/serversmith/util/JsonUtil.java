package dev.perillo.serversmith.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static <T> void save(Path path, T object) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writeValue(path.toFile(), object);
    }

    public static <T> T load(Path path, Class<T> clazz) throws IOException {
        return mapper.readValue(path.toFile(), clazz);
    }

    public static String toJson(Object object) throws IOException {
        return mapper.writeValueAsString(object);
    }
}
