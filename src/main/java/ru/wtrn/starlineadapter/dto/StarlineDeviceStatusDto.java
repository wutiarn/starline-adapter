package ru.wtrn.starlineadapter.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StarlineDeviceStatusDto {
    String alias;
    int engineTemp;
    int interiorTemp;
    int mileage;
    Instant activityTimestamp;
}
