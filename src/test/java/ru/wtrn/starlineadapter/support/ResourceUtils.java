package ru.wtrn.starlineadapter.support;

import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ResourceUtils {
    @SneakyThrows
    public static String getResourceFileAsString(String path) {
        URL resource = ResourceUtils.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException(String.format("Failed to load resource at %s", path));
        }
        try (InputStream inputStream = resource.openStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
