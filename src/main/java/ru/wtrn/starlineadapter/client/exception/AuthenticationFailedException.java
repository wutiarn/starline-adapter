package ru.wtrn.starlineadapter.client.exception;

public class AuthenticationFailedException extends Exception {
    public String responseBody;

    private AuthenticationFailedException(String message, String responseBody) {
        super(message);
        this.responseBody = responseBody;
    }

    public static AuthenticationFailedException forResponse(String responseBody) {
        return new AuthenticationFailedException(
                String.format("Starline authentication failed: %s", responseBody),
                responseBody
        );
    }
}
