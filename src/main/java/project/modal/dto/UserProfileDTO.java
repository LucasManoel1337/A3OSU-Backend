package project.modal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileDTO(
        Long idUser,
        String username,
        String email,
        String nationality,
        String language,
        Boolean verificado,
        List<String> conquistas,
        LocalDateTime criadoEm
) {}