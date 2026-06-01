package project.modal.dto;

public record UserProfileDTO(
        Long idUser,
        String username,
        String email,
        String nationality,
        String language
) {}