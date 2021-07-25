package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;
import ru.wtrn.starlineadapter.support.BaseSpringBootTest;

import java.io.File;
import java.nio.file.Files;

class StarlineClientImplTest extends BaseSpringBootTest {

    private StarlineClientImpl starlineClient;
    private File authTempFile;

    @BeforeEach
    @SneakyThrows
    void setup() {
        StarlineApiProperties properties = new StarlineApiProperties();

        authTempFile = File.createTempFile("test-starline-auth", ".txt");
        authTempFile.deleteOnExit();

        properties.setAuthCacheLocation(authTempFile.getAbsolutePath());
        properties.setBaseUrl(String.format("%s/starline", wireMockServer.baseUrl()));
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