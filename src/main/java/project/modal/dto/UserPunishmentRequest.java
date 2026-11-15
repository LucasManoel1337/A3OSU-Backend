package project.modal.dto;

import java.time.LocalDateTime;

public record UserPunishmentRequest(String type, String reason, LocalDateTime expiresAt) {
}
