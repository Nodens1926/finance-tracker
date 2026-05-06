package com.financetracker.dto.auth;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
    private boolean logoutAllDevices = false;
}
