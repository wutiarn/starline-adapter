package ru.wtrn.starlineadapter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.wtrn.starlineadapter.client.StarlineClient;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;
import ru.wtrn.starlineadapter.dto.StarlineDeviceStatusDto;

@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
public class StatusController {

    private final StarlineClient starlineClient;

    @GetMapping("/{deviceId}")
    public StarlineDeviceStatusDto getStatus(@PathVariable String deviceId) throws JsonProcessingException {
        StarlineDevice starlineDevice = starlineClient.getDevices()
                .stream()
                .filter(it -> it.getDeviceId().equals(deviceId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("Failed to find device with id %s", deviceId)));

        return StarlineDeviceStatusDto.builder()
                .alias(starlineDevice.getAlias())
                .engineTemp(starlineDevice.getEngineTemp())
                .interiorTemp(starlineDevice.getInteriorTemp())
                .build();
    }
}
