package project.modal.request;

public record ResetPasswordRequest(String email, String code, String newPassword) {}
