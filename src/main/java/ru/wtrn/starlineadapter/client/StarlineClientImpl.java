package ru.wtrn.starlineadapter.client;

import org.springframework.stereotype.Component;
import ru.wtrn.starlineadapter.client.model.StarlineDevice;

import java.util.List;

@Component
public class StarlineClientImpl implements StarlineClient {
    @Override
    public List<StarlineDevice> getDevices() {
        return null;
    }
}
