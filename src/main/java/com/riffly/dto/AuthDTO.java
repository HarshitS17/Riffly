package com.riffly.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDTO {

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                 message = "Username may only contain letters, digits, and underscores")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String displayName;
    }

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter @Builder
    public static class TokenResponse {
        private String  token;
        private String  username;
        private String  displayName;
        private long    expiresInMs;
    }
}
