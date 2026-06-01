package project.modal.dto;

public record UserProfileDTO(
        String username,
        String email,
        String nationality,
        String language
) {}