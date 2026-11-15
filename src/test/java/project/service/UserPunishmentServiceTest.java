package project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.modal.dto.UserPunishmentRequest;
import project.modal.entity.User;
import project.modal.entity.UserPunishment;
import project.repository.RefreshSessionRepository;
import project.repository.UserPunishmentRepository;
import project.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPunishmentServiceTest {

    @Mock
    private UserPunishmentRepository punishmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @InjectMocks
    private UserPunishmentService punishmentService;

    @Test
    void appliesTemporaryBanAndRevokesExistingSessions() {
        User target = user(2L, "player");
        User moderator = user(1L, "founder");
        LocalDateTime expiry = LocalDateTime.now().plusDays(2);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("founder")).thenReturn(Optional.of(moderator));
        when(punishmentRepository.findByTargetUserIdAndActiveTrueAndTypeIn(2L,
                Set.of(UserPunishmentService.PERMANENT_BAN, UserPunishmentService.TEMPORARY_BAN)))
                .thenReturn(List.of());
        when(punishmentRepository.save(any(UserPunishment.class))).thenAnswer(invocation -> {
            UserPunishment saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        var result = punishmentService.apply(2L, "founder",
                new UserPunishmentRequest(UserPunishmentService.TEMPORARY_BAN, "Abuso", expiry));

        assertThat(result.active()).isTrue();
        assertThat(result.expiresAt()).isEqualTo(expiry);
        assertThat(result.appliedByUsername()).isEqualTo("founder");
        verify(refreshSessionRepository).revokeAllByUserId(2L);
    }

    @Test
    void rejectsTemporaryBanWithoutFutureExpiry() {
        assertThatThrownBy(() -> punishmentService.apply(2L, "founder",
                new UserPunishmentRequest(UserPunishmentService.TEMPORARY_BAN, "Abuso", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data e hora futura");
    }

    @Test
    void blocksJoiningTournamentMatchingActiveSuspension() {
        UserPunishment suspension = new UserPunishment();
        suspension.setType(UserPunishmentService.RANKED_SUSPENSION);
        suspension.setReason("Conduta antidesportiva");
        suspension.setActive(true);
        suspension.setAppliedAt(LocalDateTime.now());
        suspension.setAppliedBy(user(1L, "founder"));
        when(punishmentRepository.findByTargetUserIdAndActiveTrueAndTypeIn(
                2L, Set.of(UserPunishmentService.RANKED_SUSPENSION))).thenReturn(List.of(suspension));

        ActiveTournamentSuspensionException exception = catchThrowableOfType(
                () -> punishmentService.validateTournamentEntry(2L, "ranked"),
                ActiveTournamentSuspensionException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode().value()).isEqualTo(403);
        assertThat(exception.getPunishment().reason()).isEqualTo("Conduta antidesportiva");
    }

    @Test
    void expiredSuspensionDoesNotBlockTournamentEntry() {
        UserPunishment suspension = new UserPunishment();
        suspension.setType(UserPunishmentService.CASUAL_SUSPENSION);
        suspension.setReason("Suspensão encerrada");
        suspension.setAppliedAt(LocalDateTime.now().minusDays(2));
        suspension.setExpiresAt(LocalDateTime.now().minusDays(1));
        suspension.setActive(true);
        when(punishmentRepository.findByTargetUserIdAndActiveTrueAndTypeIn(
                2L, Set.of(UserPunishmentService.CASUAL_SUSPENSION))).thenReturn(List.of(suspension));

        punishmentService.validateTournamentEntry(2L, "casual");
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
