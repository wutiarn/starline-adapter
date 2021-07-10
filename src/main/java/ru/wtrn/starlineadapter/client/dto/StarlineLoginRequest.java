package ru.wtrn.starlineadapter.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StarlineLoginRequest {
    String username;
    String password;

    @Builder.Default
    boolean rememberMe = true;
}
