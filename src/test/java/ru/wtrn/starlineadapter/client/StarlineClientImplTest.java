package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;
import ru.wtrn.starlineadapter.support.BaseSpringBootTest;
import ru.wtrn.starlineadapter.support.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        starlineClient = new StarlineClientImpl(properties, objectMapper);
    }

    @Test
    @SneakyThrows
    void testAuthInterceptorWorks() {
        wireMockServer.stubFor(post(urlEqualTo("/starline/rest/security/login"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withHeader(HttpHeaders.SET_COOKIE,
                                "lang=ru; path=/",
                                "PHPSESSID=t39lmsvr6pqwerty633hmj9c68; path=/",
                                "userAgentId=b543aee857a543392c85c72bb3qwerty; " +
                                        "expires=Wed, 03-Aug-2122 15:15:04 GMT; Max-Age=31536000; path=/"
                        )
                        .withStatus(HttpStatus.NO_CONTENT.value())
                )
        );

        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );

        List<StarlineDevice> devices = starlineClient.getDevices();
        Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

        String cachedAuth = Files.readString(authTempFile.toPath());
        Assertions.assertEquals(
                "lang=ru; PHPSESSID=t39lmsvr6pqwerty633hmj9c68; userAgentId=b543aee857a543392c85c72bb3qwerty",
                cachedAuth
        );
    }

}