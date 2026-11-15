package project.modal.dto;

import java.time.LocalDateTime;

public record UserPunishmentDTO(
        Long id,
        String type,
        String reason,
        LocalDateTime appliedAt,
        LocalDateTime expiresAt,
        boolean active,
        String appliedByUsername,
        String appliedByAvatarUrl,
        Boolean appliedByVerified,
        LocalDateTime endedAt
) {
}
