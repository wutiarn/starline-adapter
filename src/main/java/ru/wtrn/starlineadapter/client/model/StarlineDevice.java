package ru.wtrn.starlineadapter.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StarlineDevice {
    String alias;
    String deviceId;
    int engineTemp;
    int interiorTemp;
}
