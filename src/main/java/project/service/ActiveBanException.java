package project.service;

import project.modal.dto.UserPunishmentDTO;

public class ActiveBanException extends RuntimeException {
    private final UserPunishmentDTO punishment;

    public ActiveBanException(UserPunishmentDTO punishment) {
        super("A conta está temporariamente impedida de acessar a plataforma.");
        this.punishment = punishment;
    }

    public UserPunishmentDTO getPunishment() {
        return punishment;
    }
}
