package project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import project.modal.dto.UserPunishmentDTO;

public class ActiveTournamentSuspensionException extends ResponseStatusException {
    private final UserPunishmentDTO punishment;

    public ActiveTournamentSuspensionException(UserPunishmentDTO punishment) {
        super(HttpStatus.FORBIDDEN, "Você está suspenso de participar desta categoria de torneios.");
        this.punishment = punishment;
    }

    public UserPunishmentDTO getPunishment() {
        return punishment;
    }
}
