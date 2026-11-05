package project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.modal.entity.RefreshSession;
import project.modal.entity.User;
import project.repository.RefreshSessionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceTest {

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @InjectMocks
    private RefreshSessionService refreshSessionService;

    @Test
    void createsSessionWithHashedRefreshTokenAndThirtyMinuteExpiry() {
        User user = user();
        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);

        RefreshSessionService.SessionTokens tokens = refreshSessionService.create(user);

        verify(refreshSessionRepository).save(sessionCaptor.capture());
        RefreshSession savedSession = sessionCaptor.getValue();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.sessionId()).isEqualTo(savedSession.getId());
        assertThat(savedSession.getRefreshTokenHash()).hasSize(64)
                .isNotEqualTo(tokens.refreshToken());
        assertThat(savedSession.getLastActivityAt()).isNotNull();
        assertThat(savedSession.getExpiresAt()).isAfter(savedSession.getLastActivityAt())
                .isBeforeOrEqualTo(savedSession.getLastActivityAt().plusMinutes(30));
    }

    @Test
    void rotatesRefreshTokenAndSlidesSessionExpiry() {
        RefreshSession session = activeSession();
        when(refreshSessionRepository.findByRefreshTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(session));

        RefreshSessionService.SessionTokens tokens = refreshSessionService.rotate("previous-token");

        assertThat(tokens.refreshToken()).isNotEqualTo("previous-token");
        assertThat(tokens.sessionId()).isEqualTo(session.getId());
        assertThat(session.getRefreshTokenHash()).hasSize(64);
        assertThat(session.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
        assertThat(session.isRevoked()).isFalse();
    }

    @Test
    void rejectsAndRevokesInactiveSession() {
        RefreshSession session = activeSession();
        session.setLastActivityAt(LocalDateTime.now().minusMinutes(31));
        when(refreshSessionRepository.findByRefreshTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> refreshSessionService.rotate("stale-token"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(session.isRevoked()).isTrue();
    }

    private RefreshSession activeSession() {
        RefreshSession session = new RefreshSession();
        session.setId("session-id");
        session.setUser(user());
        session.setRefreshTokenHash("a".repeat(64));
        session.setLastActivityAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        session.setRevoked(false);
        return session;
    }

    private User user() {
        User user = new User();
        user.setUsername("test-user");
        return user;
    }
}
