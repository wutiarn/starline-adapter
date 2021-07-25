package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.File;
import java.nio.file.Files;

class StarlineClientImplTest {

    private final StarlineClientImpl starlineClient;
    private final File authTempFile;

    @SneakyThrows
    public StarlineClientImplTest() {
        StarlineApiProperties properties = new StarlineApiProperties();

        authTempFile = File.createTempFile("test-starline-auth", ".txt");
        authTempFile.deleteOnExit();

        properties.setAuthCacheLocation(authTempFile.getAbsolutePath());
        properties.setBaseUrl("http://localhost:8123/starline");
        properties.setUsername("test");
        properties.setPassword("testPassword");

        ObjectMapper objectMapper = new ObjectMapper();

        starlineClient = new StarlineClientImpl(properties, objectMapper);
    }

    @Test
    @SneakyThrows
    void testAuthInterceptorWorks() {
        starlineClient.getDevices();

        String cachedAuth = Files.readString(authTempFile.toPath());
        Assertions.assertFalse(cachedAuth.isBlank());
    }

}