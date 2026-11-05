package project.service;

import project.modal.response.AuthResponse;

public record AuthSessionResult(AuthResponse response, String refreshToken) {
}
