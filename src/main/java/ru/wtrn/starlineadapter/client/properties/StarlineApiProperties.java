package ru.wtrn.starlineadapter.client.properties;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StarlineApiProperties {
    String baseUrl;
    String username;
    String password;

    @Builder.Default
    String authCacheLocation = "starline-auth.txt";
}
