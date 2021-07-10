package ru.wtrn.starlineadapter;

import ru.wtrn.starlineadapter.model.StarlineDevice;

import java.util.List;

public interface StarlineClient {
    List<StarlineDevice> getDevices();
}
