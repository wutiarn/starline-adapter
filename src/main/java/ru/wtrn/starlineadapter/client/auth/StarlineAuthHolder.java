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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class StarlineAuthHolder {
    private final StarlineApiProperties properties;
    private final RestTemplate httpClient;
    private Path authCacheFile;
    private String cookieHeader;

    public StarlineAuthHolder(StarlineApiProperties properties) throws IOException {
        this.properties = properties;
        httpClient = new RestTemplateBuilder()
                .rootUri(properties.getBaseUrl())
                .build();

        String authCacheLocation = properties.getAuthCacheLocation();
        if (authCacheLocation != null) {
            authCacheFile = Path.of(authCacheLocation);
            if (Files.exists(authCacheFile)) {
                cookieHeader = Files.readString(authCacheFile);
            }
        }
    }

    public HttpRequest addAuthenticationToRequest(HttpRequest request) throws AuthenticationFailedException, IOException {
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(HttpHeaders.COOKIE, getCookieHeader());
        return wrapper;
    }

    public void refreshAuthentication() throws AuthenticationFailedException, IOException {
        log.info("Forced authentication refresh started");
        doRefreshAuthentication();
    }

    private synchronized String getCookieHeader() throws AuthenticationFailedException, IOException {
        if (cookieHeader == null) {
            return doRefreshAuthentication();
        }
        return cookieHeader;
    }

    private synchronized String doRefreshAuthentication() throws AuthenticationFailedException, IOException {
        StarlineLoginRequest loginRequest = StarlineLoginRequest.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();

        ResponseEntity<String> response = httpClient.postForEntity("/rest/security/login", loginRequest, String.class);
        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            throw AuthenticationFailedException.forResponse(response.getBody());
        }

        List<String> setCookieValue = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookieValue == null) {
            throw new IllegalStateException("Successful login request did not return Set-Cookie headers");
        }

        String cookieHeader = composeCookieHeader(setCookieValue);
        this.cookieHeader = cookieHeader;

        if (authCacheFile != null) {
            Files.writeString(authCacheFile, cookieHeader);
        }

        log.info("Starline authentication refreshed");
        return cookieHeader;
    }

    private String composeCookieHeader(List<String> setCookieValues) {
        return setCookieValues.stream()
                // Transform "lang=ru; path=/" -> "lang=ru"
                .map(it -> Arrays.stream(it.split(";")).findFirst().orElseThrow())
                // And join to one cookies string, that can be used as Cookies header value.
                .collect(Collectors.joining("; "));
    }
}
