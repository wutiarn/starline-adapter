package ru.wtrn.starlineadapter.client;

import ru.wtrn.starlineadapter.client.model.StarlineDevice;

import java.util.List;

public interface StarlineClient {
    List<StarlineDevice> getDevices();
}
