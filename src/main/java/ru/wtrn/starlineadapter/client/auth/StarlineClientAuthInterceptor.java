package ru.wtrn.starlineadapter.client.auth;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import ru.wtrn.starlineadapter.client.exception.AuthenticationFailedException;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.IOException;

public class StarlineClientAuthInterceptor implements ClientHttpRequestInterceptor {
    private StarlineAuthHolder authHolder;

    public StarlineClientAuthInterceptor(StarlineApiProperties properties) throws IOException {
        authHolder = new StarlineAuthHolder(properties);
    }

    @Override
    @SneakyThrows
    public ClientHttpResponse intercept(
            @NotNull HttpRequest httpRequest,
            byte @NotNull [] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        HttpRequest wrappedRequest = authHolder.addAuthenticationToRequest(httpRequest);
        ClientHttpResponse response = execution.execute(wrappedRequest, body);

        if (checkResponseRequiresAuthenticationRefresh(response)) {
            return retryRequestWithRefreshedCredentials(httpRequest, body, execution);
        }

        return null;
    }

    private boolean checkResponseRequiresAuthenticationRefresh(ClientHttpResponse response) throws IOException {
        switch (response.getStatusCode()) {
            case TEMPORARY_REDIRECT, UNAUTHORIZED, FORBIDDEN -> {
                return true;
            }
        }

        MediaType contentType = response.getHeaders().getContentType();
        return contentType != null && !contentType.includes(MediaType.APPLICATION_JSON);
    }

    ClientHttpResponse retryRequestWithRefreshedCredentials(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws AuthenticationFailedException, IOException {
        authHolder.refreshAuthentication();
        HttpRequest wrappedRequest = authHolder.addAuthenticationToRequest(request);
        return execution.execute(wrappedRequest, body);
    }
}
