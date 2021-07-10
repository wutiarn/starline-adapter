package ru.wtrn.starlineadapter.client.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class StarlineAuthHolder {

    private RestTemplate httpClient;
    private Path authCacheFile;
    private String cookieHeader;
    private ReentrantLock cookieHeaderLock = new ReentrantLock();

    public StarlineAuthHolder(StarlineApiProperties properties) throws IOException {
        httpClient = new RestTemplateBuilder()
                .rootUri(properties.getBaseUrl())
                .build();

        authCacheFile = Path.of(properties.getAuthCacheLocation());
        if (Files.exists(authCacheFile)) {
            cookieHeader = Files.readString(authCacheFile);
        }
    }

    HttpRequest addAuthenticationToRequest(HttpRequest request) {
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(HttpHeaders.COOKIE, );
    }

    private String getCookieHeader() {
        cookieHeaderLock
    }

    private String doRefreshAuthentication() {

    }
}
