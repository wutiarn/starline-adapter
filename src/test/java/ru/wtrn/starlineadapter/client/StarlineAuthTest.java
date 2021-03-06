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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class StarlineAuthTest extends BaseSpringBootTest {

    private StarlineClientImpl starlineClient;
    private Path authTempFilePath;
    private final StarlineApiProperties properties = new StarlineApiProperties();
    private final String expectedAuthCookie =
            "lang=ru; PHPSESSID=t39lmsvr6pqwerty633hmj9c68; userAgentId=b543aee857a543392c85c72bb3qwerty";

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    @SneakyThrows
    void setup() {
        File tempFile = File.createTempFile("test-starline-auth", ".txt");
        tempFile.deleteOnExit();
        authTempFilePath = tempFile.toPath();

        properties.setAuthCacheLocation(authTempFilePath.toString());
        properties.setBaseUrl(String.format("%s/starline", wireMockServer.baseUrl()));
        properties.setUsername("test");
        properties.setPassword("testPassword");

        starlineClient = new StarlineClientImpl(properties, objectMapper);
    }

    private void stubStarlineLoginEndpoint() {
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
    }

    @Test
    @SneakyThrows
    void testAuthInterceptorWorks() {
        stubStarlineLoginEndpoint();
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );

        List<StarlineDevice> devices = starlineClient.getDevices();
        Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

        String cachedAuth = Files.readString(authTempFilePath);
        Assertions.assertEquals(
                expectedAuthCookie,
                cachedAuth
        );

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedAuthCookie)));

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

    @Test
    @SneakyThrows
    void testAuthExpired() {
        stubStarlineLoginEndpoint();
        String invalidCookie = "lang=ru";
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedAuthCookie))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(invalidCookie))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withStatus(HttpStatus.OK.value())
                )
        );
        Files.writeString(authTempFilePath, invalidCookie);
        // Reinitialize starlineClient to make StarlineAuthHolder read modified authTempFile
        starlineClient = new StarlineClientImpl(properties, objectMapper);

        List<StarlineDevice> devices = starlineClient.getDevices();
        Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

        // /login endpoint should be called to refresh invalid authentication
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/starline/rest/security/login")));

        // /device endpoint should be called twice: first time with invalid auth, and then with refreshed one
        wireMockServer.verify(2, getRequestedFor(urlEqualTo("/starline/device")));

        // Check that these two requests were made with different cookie headers
        Set<String> actualRequestCookies = wireMockServer.getServeEvents().getRequests().stream()
                .filter(it -> it.getRequest().getUrl().equals("/starline/device"))
                .map(it -> it.getRequest().getHeader(HttpHeaders.COOKIE))
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(expectedAuthCookie, invalidCookie), actualRequestCookies);
    }


    @Test
    @SneakyThrows
    void testCachedAuthUsed() {
        stubStarlineLoginEndpoint();
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedAuthCookie))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );

        Files.writeString(authTempFilePath, expectedAuthCookie);
        // Reinitialize starlineClient to make StarlineAuthHolder read modified authTempFile
        starlineClient = new StarlineClientImpl(properties, objectMapper);

        List<StarlineDevice> devices = starlineClient.getDevices();
        Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

        // /login endpoint should not be called, since valid auth is cached
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/starline/rest/security/login")));

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedAuthCookie)));
    }

    @Test
    @SneakyThrows
    void testLoginResponseWithoutSetCookie() {
        wireMockServer.stubFor(post(urlEqualTo("/starline/rest/security/login"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.NO_CONTENT.value())
                )
        );

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> starlineClient.getDevices()
        );

        Assertions.assertEquals("Successful login request did not return Set-Cookie headers", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void testWorksWithoutCache() {
        stubStarlineLoginEndpoint();
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );

        properties.setAuthCacheLocation(null);
        // Reinitialize starlineClient to make StarlineAuthHolder uses updated properties
        starlineClient = new StarlineClientImpl(properties, objectMapper);

        List<StarlineDevice> devices = starlineClient.getDevices();
        Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

        // /login endpoint should be called before /device
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/starline/rest/security/login")));

        // /device should be called exactly once, and with correct authentication
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/starline/device"))
                .withHeader(HttpHeaders.COOKIE, equalTo(expectedAuthCookie)));
    }

    @Test
    @SneakyThrows
    void testCreatesAuthCacheFile() {
        stubStarlineLoginEndpoint();
        wireMockServer.stubFor(get(urlEqualTo("/starline/device"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(ResourceUtils.getResourceFileAsString("/reference/starline/devices.json"))
                )
        );

        Files.delete(authTempFilePath);
        Assertions.assertFalse(Files.exists(authTempFilePath));

        // Reinitialize starlineClient
        starlineClient = new StarlineClientImpl(properties, objectMapper);
        // Check that auth cache file creation is lazy
        Assertions.assertFalse(Files.exists(authTempFilePath));

        try {
            List<StarlineDevice> devices = starlineClient.getDevices();
            Assertions.assertEquals("860920000000000", devices.get(0).getDeviceId());

            Assertions.assertTrue(Files.exists(authTempFilePath));
            String cachedAuth = Files.readString(authTempFilePath);
            Assertions.assertEquals(expectedAuthCookie, cachedAuth);
        } finally {
            Files.delete(authTempFilePath);
        }
    }
}
