package ru.wtrn.starlineadapter.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("SpellCheckingInspection")
public class StarlineDeviceInfoResponse {

    Answer answer;

    @Data
    public static class Answer {
        List<Device> devices;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Device {
        String alias;
        String deviceId;
        int ctemp;
        int etemp;
    }
}
