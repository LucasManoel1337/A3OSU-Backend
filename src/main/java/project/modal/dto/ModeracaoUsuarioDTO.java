package project.modal.dto;

import java.util.List;

public record ModeracaoUsuarioDTO(
        Long idUser,
        String username,
        String avatarUrl,
        String nacionalidade,
        Boolean verificado,
        List<String> conquistas,
        List<String> titulos,
        List<UserPunishmentDTO> punicoes
) {}
