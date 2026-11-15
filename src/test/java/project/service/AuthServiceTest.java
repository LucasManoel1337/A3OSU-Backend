package project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.modal.entity.AuthVerificationCode;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.modal.dto.UserPunishmentDTO;
import project.modal.request.LoginRequest;
import project.modal.entity.VerificationPurpose;
import project.modal.request.ForgotPasswordRequest;
import project.repository.AuthVerificationCodeRepository;
import project.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthVerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshSessionService refreshSessionService;

    @Mock
    private ResendEmailService emailService;

    @Mock
    private UserPunishmentService punishmentService;

    @InjectMocks
    private AuthService authService;

    @Test
    void replacesExistingPasswordResetCodeWithoutInsertingDuplicateRow() {
        User user = new User();
        user.setId(2L);
        user.setEmail("player@example.com");

        AuthVerificationCode existingCode = new AuthVerificationCode();
        existingCode.setId(10L);
        existingCode.setUser(user);
        existingCode.setPurpose(VerificationPurpose.PASSWORD_RESET);
        existingCode.setAttempts(4);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findByUser_IdAndPurpose(2L, VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(existingCode));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        authService.solicitarRedefinicaoSenha(new ForgotPasswordRequest("PLAYER@example.com"));

        assertThat(existingCode.getId()).isEqualTo(10L);
        assertThat(existingCode.getCodeHash()).isEqualTo("hashed-code");
        assertThat(existingCode.getAttempts()).isZero();
        assertThat(existingCode.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(verificationCodeRepository).save(existingCode);
        verify(verificationCodeRepository, never()).deleteByUser_IdAndPurpose(
                2L, VerificationPurpose.PASSWORD_RESET);
        verify(emailService).enviarCodigo(eq("player@example.com"), anyString(),
                eq(VerificationPurpose.PASSWORD_RESET));
    }

    @Test
    void blocksLoginAndReturnsActiveBanDetails() {
        User user = new User();
        user.setId(7L);
        user.setEmail("player@example.com");
        user.setUsername("player");
        UserDetalhes details = new UserDetalhes();
        details.setEmailVerificado(true);
        user.setDetalhes(details);
        UserPunishmentDTO punishment = new UserPunishmentDTO(
                3L, UserPunishmentService.TEMPORARY_BAN, "Motivo", LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), true, "founder", null, true, null);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(punishmentService.findActiveBan(7L)).thenReturn(Optional.of(punishment));

        assertThatThrownBy(() -> authService.login(new LoginRequest("PLAYER@example.com", "password")))
                .isInstanceOf(ActiveBanException.class)
                .satisfies(exception -> assertThat(((ActiveBanException) exception).getPunishment())
                        .isEqualTo(punishment));
        verify(refreshSessionService, never()).create(user);
    }
}
