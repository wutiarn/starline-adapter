package ru.wtrn.starlineadapter.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("ru.wtrn.starline-adapter.api")
public class StarlineApiProperties {
    String baseUrl;
    String username;
    String password;
    String authCacheLocation = "starline-auth.txt";
}
