package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.wtrn.starlineadapter.client.dto.StarlineLoginRequest;
import ru.wtrn.starlineadapter.client.exception.AuthenticationFailedException;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;
import ru.wtrn.starlineadapter.support.BaseSpringBootTest;
import ru.wtrn.starlineadapter.support.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class StarlineAuthTest extends BaseSpringBootTest {

    private StarlineClientImpl starlineClient;
    private File authTempFile;
    private final StarlineApiProperties properties = new StarlineApiProperties();

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    @SneakyThrows
    void setup() {
        authTempFile = File.createTempFile("test-starline-auth", ".txt");
        authTempFile.deleteOnExit();

        properties.setAuthCacheLocation(authTempFile.getAbsolutePath());
        properties.setBaseUrl(String.format("%s/starline", wireMockServer.baseUrl()));
        properties.setUsername("test");
        properties.setPassword("testPassword");

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

        String expectedCookie = "lang=ru; PHPSESSID=t39lmsvr6pqwerty633hmj9c68; userAgentId=b543aee857a543392c85c72bb3qwerty";
        String cachedAuth = Files.readString(authTempFile.toPath());
        Assertions.assertEquals(
                expectedCookie,
                cachedAuth
        );

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedCookie)));

        StarlineLoginRequest expectedLoginRequest = StarlineLoginRequest.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/starline/rest/security/login"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(expectedLoginRequest))));
    }

    @Test
    @SneakyThrows
    void testIncorrectPassword() {
        String loginFailedBody = ResourceUtils.getResourceFileAsString("/reference/starline/login/incorrectPassword.json");
        wireMockServer.stubFor(post(urlEqualTo("/starline/rest/security/login"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withHeader(HttpHeaders.SET_COOKIE,
                                "lang=ru; path=/",
                                "PHPSESSID=t39lmsvr6pqwerty633hmj9c68; path=/"
                        )
                        .withStatus(HttpStatus.OK.value())
                        .withBody(loginFailedBody)
                )
        );
        AuthenticationFailedException exception = Assertions.assertThrows(
                AuthenticationFailedException.class,
                () -> starlineClient.getDevices()
        );
        Assertions.assertEquals(loginFailedBody, exception.responseBody);
    }
}
