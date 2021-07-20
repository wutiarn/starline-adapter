package ru.wtrn.starlineadapter.client.properties;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Builder
@ConfigurationProperties("ru.wtrn.starline-adapter")
public class StarlineApiProperties {
    String baseUrl;
    String username;
    String password;

    @Builder.Default
    String authCacheLocation = "starline-auth.txt";
}
