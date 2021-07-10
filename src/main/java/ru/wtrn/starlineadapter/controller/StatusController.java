package ru.wtrn.starlineadapter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.wtrn.starlineadapter.dto.StarlineDeviceStatusDto;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {
    @GetMapping("/{deviceId}")
    public StarlineDeviceStatusDto getStatus(@PathVariable String deviceId) {
        return StarlineDeviceStatusDto.builder()
                .build();
    }
}
