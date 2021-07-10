package ru.wtrn.starlineadapter.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StarlineDevice {
    String alias;
    int engineTemp;
    int interiorTemp;
    int mileage;
}
