package project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import project.modal.dto.UserPunishmentDTO;
import project.modal.dto.UserPunishmentRequest;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.modal.entity.UserPunishment;
import project.repository.RefreshSessionRepository;
import project.repository.UserPunishmentRepository;
import project.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserPunishmentService {

    public static final String PERMANENT_BAN = "BANIMENTO_PERMANENTE";
    public static final String TEMPORARY_BAN = "BANIMENTO_TEMPORARIO";
    public static final String RANKED_SUSPENSION = "SUSPENSAO_RANQUEADO";
    public static final String CASUAL_SUSPENSION = "SUSPENSAO_CASUAL";

    private static final Set<String> TYPES = Set.of(
            PERMANENT_BAN, TEMPORARY_BAN, RANKED_SUSPENSION, CASUAL_SUSPENSION);
    private static final Set<String> BAN_TYPES = Set.of(PERMANENT_BAN, TEMPORARY_BAN);

    private final UserPunishmentRepository punishmentRepository;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    public UserPunishmentService(
            UserPunishmentRepository punishmentRepository,
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository) {
        this.punishmentRepository = punishmentRepository;
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
    }

    @Transactional
    public UserPunishmentDTO apply(Long targetUserId, String moderatorUsername, UserPunishmentRequest request) {
        String type = request == null || request.type() == null ? "" : request.type().trim();
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        LocalDateTime expiresAt = request == null ? null : request.expiresAt();
        LocalDateTime now = LocalDateTime.now();

        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("Selecione um tipo de punição válido.");
        }
        if (reason.isBlank() || reason.length() > 1000
                || reason.chars().anyMatch(character ->
                Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t')) {
            throw new IllegalArgumentException("O motivo é obrigatório e deve ter até 1000 caracteres.");
        }
        if (TEMPORARY_BAN.equals(type) && (expiresAt == null || !expiresAt.isAfter(now))) {
            throw new IllegalArgumentException("Informe uma data e hora futura para o banimento temporário.");
        }
        if (PERMANENT_BAN.equals(type) && expiresAt != null) {
            throw new IllegalArgumentException("O banimento permanente não pode ter data de término.");
        }
        if ((RANKED_SUSPENSION.equals(type) || CASUAL_SUSPENSION.equals(type))
                && expiresAt != null && !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("A data e hora de término da suspensão deve estar no futuro.");
        }

        User target = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogador não encontrado."));
        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderador não encontrado."));

        Set<String> conflictingTypes = BAN_TYPES.contains(type) ? BAN_TYPES : Set.of(type);
        boolean alreadyActive = punishmentRepository
                .findByTargetUserIdAndActiveTrueAndTypeIn(targetUserId, conflictingTypes)
                .stream()
                .anyMatch(punishment -> isEffective(punishment, now));
        if (alreadyActive) {
            throw new IllegalArgumentException("O jogador já possui essa punição ativa.");
        }

        UserPunishment punishment = new UserPunishment();
        punishment.setTargetUser(target);
        punishment.setAppliedBy(moderator);
        punishment.setType(type);
        punishment.setReason(reason);
        punishment.setAppliedAt(now);
        punishment.setExpiresAt(expiresAt);
        punishment.setActive(true);
        UserPunishment saved = punishmentRepository.save(punishment);

        if (BAN_TYPES.contains(type)) {
            refreshSessionRepository.revokeAllByUserId(targetUserId);
        }
        return toDto(saved, now);
    }

    @Transactional
    public UserPunishmentDTO revoke(Long punishmentId, String moderatorUsername) {
        UserPunishment punishment = punishmentRepository.findById(punishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punição não encontrada."));
        LocalDateTime now = LocalDateTime.now();
        if (!isEffective(punishment, now)) {
            throw new IllegalArgumentException("Essa punição não está ativa.");
        }

        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderador não encontrado."));
        punishment.setActive(false);
        punishment.setEndedAt(now);
        punishment.setEndedBy(moderator);
        UserPunishment saved = punishmentRepository.save(punishment);
        return toDto(saved, now);
    }

    @Transactional(readOnly = true)
    public List<UserPunishmentDTO> listForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return punishmentRepository.findByTargetUserIdOrderByAppliedAtDesc(userId).stream()
                .map(punishment -> toDto(punishment, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserPunishmentDTO> findActiveBan(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return punishmentRepository.findByTargetUserIdAndActiveTrueAndTypeIn(userId, BAN_TYPES).stream()
                .filter(punishment -> isEffective(punishment, now))
                .findFirst()
                .map(punishment -> toDto(punishment, now));
    }

    @Transactional(readOnly = true)
    public void validateTournamentEntry(Long userId, String tournamentType) {
        String suspensionType = "ranked".equalsIgnoreCase(tournamentType)
                ? RANKED_SUSPENSION
                : "casual".equalsIgnoreCase(tournamentType) ? CASUAL_SUSPENSION : null;
        if (suspensionType == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        punishmentRepository.findByTargetUserIdAndActiveTrueAndTypeIn(userId, Set.of(suspensionType)).stream()
                .filter(punishment -> isEffective(punishment, now))
                .findFirst()
                .ifPresent(punishment -> { throw new ActiveTournamentSuspensionException(toDto(punishment, now)); });
    }

    public UserPunishmentDTO toDto(UserPunishment punishment) {
        return toDto(punishment, LocalDateTime.now());
    }

    private UserPunishmentDTO toDto(UserPunishment punishment, LocalDateTime now) {
        User moderator = punishment.getAppliedBy();
        UserDetalhes details = moderator.getDetalhes();
        String avatar = details == null || details.getAvatarData() == null
                ? null
                : "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(details.getAvatarData());
        Boolean verified = details != null && Boolean.TRUE.equals(details.getVerificado());
        return new UserPunishmentDTO(
                punishment.getId(),
                punishment.getType(),
                punishment.getReason(),
                punishment.getAppliedAt(),
                punishment.getExpiresAt(),
                isEffective(punishment, now),
                moderator.getUsername(),
                avatar,
                verified,
                punishment.getEndedAt()
        );
    }

    private boolean isEffective(UserPunishment punishment, LocalDateTime now) {
        return punishment.isActive()
                && (punishment.getExpiresAt() == null || punishment.getExpiresAt().isAfter(now));
    }
}
