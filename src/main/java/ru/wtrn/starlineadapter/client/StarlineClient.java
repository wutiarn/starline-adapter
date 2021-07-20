package ru.wtrn.starlineadapter.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;

import java.util.List;

public interface StarlineClient {
    List<StarlineDevice> getDevices() throws JsonProcessingException;
}
