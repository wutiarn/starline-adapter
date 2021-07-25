package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.File;

class StarlineClientImplTest {

    private final StarlineClientImpl starlineClient;

    @SneakyThrows
    public StarlineClientImplTest() {
        StarlineApiProperties properties = new StarlineApiProperties();

        File tempFile = File.createTempFile("test-starline-auth", ".txt");
        tempFile.deleteOnExit();

        properties.setAuthCacheLocation(tempFile.getAbsolutePath());
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
    }

}