package project.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import project.modal.entity.RefreshSession;
import project.modal.entity.User;
import project.repository.RefreshSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshSessionService {

    private static final Duration INACTIVITY_TIMEOUT = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVALID_SESSION_MESSAGE = "Sessão inválida ou expirada.";

    private final RefreshSessionRepository refreshSessionRepository;

    public RefreshSessionService(RefreshSessionRepository refreshSessionRepository) {
        this.refreshSessionRepository = refreshSessionRepository;
    }

    @Transactional
    public SessionTokens create(User user) {
        LocalDateTime now = LocalDateTime.now();
        String refreshToken = generateRefreshToken();

        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setRefreshTokenHash(hash(refreshToken));
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plus(INACTIVITY_TIMEOUT));
        session.setRevoked(false);
        refreshSessionRepository.save(session);

        return new SessionTokens(session.getId(), user, refreshToken);
    }

    @Transactional(dontRollbackOn = ResponseStatusException.class)
    public SessionTokens rotate(String refreshToken) {
        RefreshSession session = findSession(refreshToken)
                .orElseThrow(this::invalidSession);
        LocalDateTime now = LocalDateTime.now();
        if (!isActive(session, now)) {
            session.setRevoked(true);
            throw invalidSession();
        }

        String newRefreshToken = generateRefreshToken();
        session.setRefreshTokenHash(hash(newRefreshToken));
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plus(INACTIVITY_TIMEOUT));
        return new SessionTokens(session.getId(), session.getUser(), newRefreshToken);
    }

    @Transactional
    public boolean recordActivity(String sessionId, String username) {
        Optional<RefreshSession> found = refreshSessionRepository.findById(sessionId);
        if (found.isEmpty()) {
            return false;
        }

        RefreshSession session = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (!session.getUser().getUsername().equals(username) || !isActive(session, now)) {
            session.setRevoked(true);
            return false;
        }

        session.setLastActivityAt(now);
        session.setExpiresAt(now.plus(INACTIVITY_TIMEOUT));
        return true;
    }

    @Transactional
    public void revoke(String refreshToken) {
        findSession(refreshToken).ifPresent(session -> session.setRevoked(true));
    }

    private Optional<RefreshSession> findSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        return refreshSessionRepository.findByRefreshTokenHashForUpdate(hash(refreshToken));
    }

    private boolean isActive(RefreshSession session, LocalDateTime now) {
        return !session.isRevoked()
                && session.getExpiresAt().isAfter(now)
                && session.getLastActivityAt().isAfter(now.minus(INACTIVITY_TIMEOUT));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }

    private ResponseStatusException invalidSession() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_SESSION_MESSAGE);
    }

    public record SessionTokens(String sessionId, User user, String refreshToken) {
    }
}
