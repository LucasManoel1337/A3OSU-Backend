package project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.repository.UserDetalhesRepository;
import project.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetalhesRepository userDetalhesRepository;

    @Mock
    private UserPunishmentService punishmentService;

    @InjectMocks
    private UserService userService;

    @Test
    void deniesModerationSearchWithoutFounderAchievement() {
        User user = new User();
        user.setUsername("player");
        user.setDetalhes(new UserDetalhes());
        when(userRepository.findByUsername("player")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.pesquisarUsuariosParaModeracao("player", "al"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void verificationBadgeCanChangeWithoutChangingEmailVerification() {
        User founder = userWithAchievement("founder", "FOUNDER");
        User target = new User();
        target.setId(2L);
        UserDetalhes targetDetails = new UserDetalhes();
        targetDetails.setEmailVerificado(true);

        when(userRepository.findByUsername("founder")).thenReturn(Optional.of(founder));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userDetalhesRepository.findByUserId(2L)).thenReturn(Optional.of(targetDetails));

        userService.atualizarVerificado("founder", 2L, true);

        assertThat(targetDetails.getVerificado()).isTrue();
        assertThat(targetDetails.getEmailVerificado()).isTrue();
        verify(userDetalhesRepository).save(targetDetails);
    }

    @Test
    void deniesPunishmentActionsWithoutFounderAchievement() {
        User player = userWithAchievement("player", "MEMBRO_BETA");
        when(userRepository.findByUsername("player")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> userService.aplicarPunicao("player", 2L,
                new project.modal.dto.UserPunishmentRequest(
                        UserPunishmentService.PERMANENT_BAN, "Motivo", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User userWithAchievement(String username, String achievement) {
        User user = new User();
        user.setUsername(username);
        UserDetalhes details = new UserDetalhes();
        details.getConquistas().add(achievement);
        details.setUser(user);
        user.setDetalhes(details);
        return user;
    }
}
