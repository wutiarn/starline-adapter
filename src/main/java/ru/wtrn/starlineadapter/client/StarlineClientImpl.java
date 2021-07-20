package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.wtrn.starlineadapter.client.auth.StarlineClientAuthInterceptor;
import ru.wtrn.starlineadapter.client.dto.StarlineDeviceInfoResponse;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;
import ru.wtrn.starlineadapter.client.properties.StarlineApiProperties;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StarlineClientImpl implements StarlineClient {

    private final RestTemplate httpClient;
    private final ObjectMapper objectMapper;

    public StarlineClientImpl(StarlineApiProperties properties, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        StarlineClientAuthInterceptor authInterceptor = new StarlineClientAuthInterceptor(properties);
        httpClient = new RestTemplateBuilder()
                .rootUri(properties.getBaseUrl())
                .interceptors(authInterceptor)
                .build();
    }

    @Override
    public List<StarlineDevice> getDevices() throws JsonProcessingException {
        ResponseEntity<String> response = httpClient.getForEntity("/device", String.class);
        String body = response.getBody();
        log.debug("Devices response: {}", body);

        StarlineDeviceInfoResponse responseDto = objectMapper.readValue(body, StarlineDeviceInfoResponse.class);
        return responseDto.getAnswer().getDevices().stream().map(device ->
                StarlineDevice.builder()
                        .alias(device.getAlias())
                        .deviceId(device.getDeviceId())
                        .engineTemp(device.getEtemp())
                        .interiorTemp(device.getCtemp())
                        .build()
        ).collect(Collectors.toList());
    }
}
