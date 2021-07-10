package ru.wtrn.starlineadapter.client.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import ru.wtrn.starlineadapter.client.dto.StarlineLoginRequest;
import ru.wtrn.starlineadapter.client.exception.AuthenticationFailedException;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class StarlineAuthHolder {
    private StarlineApiProperties properties;
    private RestTemplate httpClient;
    private Path authCacheFile;
    private String cookieHeader;

    public StarlineAuthHolder(StarlineApiProperties properties) throws IOException {
        httpClient = new RestTemplateBuilder()
                .rootUri(properties.getBaseUrl())
                .build();

        authCacheFile = Path.of(properties.getAuthCacheLocation());
        if (Files.exists(authCacheFile)) {
            cookieHeader = Files.readString(authCacheFile);
        }
    }

    HttpRequest addAuthenticationToRequest(HttpRequest request) throws AuthenticationFailedException {
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(HttpHeaders.COOKIE, getCookieHeader());
        return wrapper;
    }

    private synchronized String getCookieHeader() throws AuthenticationFailedException {
        if (cookieHeader == null) {
            return doRefreshAuthentication();
        }
        return cookieHeader;
    }

    private synchronized String doRefreshAuthentication() throws AuthenticationFailedException {
        StarlineLoginRequest loginRequest = StarlineLoginRequest.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();

        ResponseEntity<String> response = httpClient.postForEntity("/rest/security/login", loginRequest, String.class);
        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            throw AuthenticationFailedException.forResponse(response.getBody());
        }

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null) {
            throw new IllegalStateException("Successful login request did not return Set-Cookie headers");
        }
    }
}
