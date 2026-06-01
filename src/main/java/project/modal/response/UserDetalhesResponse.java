package project.modal.response;

public record UserDetalhesResponse(
        String avatarUrl, // Se for servir como base64 ou URL de rota
        String bannerUrl
) {}