package project.service;

import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import project.modal.response.AuthResponse;
import project.modal.response.MessageResponse;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
import project.modal.entity.AuthVerificationCode;
import project.modal.entity.VerificationPurpose;
import project.repository.UserRepository;
import project.repository.AuthVerificationCodeRepository;
import project.modal.request.CadastroRequest;
import project.modal.request.ForgotPasswordRequest;
import project.modal.request.LoginRequest;
import project.modal.request.ResetPasswordRequest;
import project.modal.request.ResendVerificationRequest;
import project.modal.request.VerifyEmailRequest;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class AuthService {

    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVALID_CODE_MESSAGE = "Código inválido ou expirado.";

    private final UserRepository userRepository;
    private final AuthVerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshSessionService refreshSessionService;
    private final ResendEmailService emailService;

    public AuthService(UserRepository userRepository,
                       AuthVerificationCodeRepository verificationCodeRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshSessionService refreshSessionService,
                       ResendEmailService emailService) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshSessionService = refreshSessionService;
        this.emailService = emailService;
    }

    @Transactional
    public MessageResponse registrar(CadastroRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário ou e-mail já cadastrado.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));

        UserDetalhes detalhes = new UserDetalhes();
        detalhes.setNationality(request.nationality());
        detalhes.setLanguage(request.language());
        detalhes.setUsername(request.username());
        detalhes.setVerificado(false);
        detalhes.setUser(user);
        detalhes.setCriadoEm(LocalDateTime.now());

        detalhes.getConquistas().add("MEMBRO_BETA");

        user.setDetalhes(detalhes);

        userRepository.save(user);

        enviarCodigo(user, VerificationPurpose.EMAIL_VERIFICATION);
        return new MessageResponse("Cadastro realizado. Verifique seu email para ativar a conta.");
    }

    @Transactional
    public AuthSessionResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        if (user.getDetalhes() == null || !Boolean.TRUE.equals(user.getDetalhes().getVerificado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu email antes de entrar.");
        }

        RefreshSessionService.SessionTokens session = refreshSessionService.create(user);
        String token = jwtService.generateToken(user, session.sessionId());
        return new AuthSessionResult(new AuthResponse(token, user.getUsername()), session.refreshToken());
    }

    public AuthSessionResult refresh(String refreshToken) {
        RefreshSessionService.SessionTokens session = refreshSessionService.rotate(refreshToken);
        String token = jwtService.generateToken(session.user(), session.sessionId());
        return new AuthSessionResult(
                new AuthResponse(token, session.user().getUsername()),
                session.refreshToken()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshSessionService.revoke(refreshToken);
    }

    @Transactional(dontRollbackOn = ResponseStatusException.class)
    public MessageResponse verificarEmail(VerifyEmailRequest request) {
        User user = findUserByEmail(request.email());
        AuthVerificationCode code = validarCodigo(user, request.code(), VerificationPurpose.EMAIL_VERIFICATION);

        user.getDetalhes().setVerificado(true);
        verificationCodeRepository.delete(code);
        return new MessageResponse("Email confirmado com sucesso.");
    }

    @Transactional
    public MessageResponse reenviarVerificacao(ResendVerificationRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(user -> user.getDetalhes() == null
                        || !Boolean.TRUE.equals(user.getDetalhes().getVerificado()))
                .ifPresent(user -> enviarCodigo(user, VerificationPurpose.EMAIL_VERIFICATION));

        return new MessageResponse("Se a conta existir e ainda não estiver confirmada, enviaremos um novo código.");
    }

    @Transactional
    public MessageResponse solicitarRedefinicaoSenha(ForgotPasswordRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email())).ifPresent(user ->
                enviarCodigo(user, VerificationPurpose.PASSWORD_RESET));

        return new MessageResponse("Se o email estiver cadastrado, enviaremos um código de redefinição.");
    }

    @Transactional(dontRollbackOn = ResponseStatusException.class)
    public MessageResponse redefinirSenha(ResetPasswordRequest request) {
        User user = findUserByEmail(request.email());
        AuthVerificationCode code = validarCodigo(user, request.code(), VerificationPurpose.PASSWORD_RESET);

        if (request.newPassword() == null || !request.newPassword().matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter de 6 a 20 caracteres, incluindo maiúscula, minúscula, número e símbolo.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        verificationCodeRepository.delete(code);
        return new MessageResponse("Senha redefinida com sucesso.");
    }

    private void enviarCodigo(User user, VerificationPurpose purpose) {
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("Conta não encontrada para envio do código."));

        String rawCode = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        AuthVerificationCode code = verificationCodeRepository
                .findByUser_IdAndPurpose(lockedUser.getId(), purpose)
                .orElseGet(AuthVerificationCode::new);
        code.setUser(lockedUser);
        code.setPurpose(purpose);
        code.setCodeHash(passwordEncoder.encode(rawCode));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
        code.setAttempts(0);
        verificationCodeRepository.save(code);

        emailService.enviarCodigo(lockedUser.getEmail(), rawCode, purpose);
    }

    private AuthVerificationCode validarCodigo(User user, String rawCode, VerificationPurpose purpose) {
        AuthVerificationCode code = verificationCodeRepository.findByUser_IdAndPurpose(user.getId(), purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE_MESSAGE));

        if (code.getExpiresAt().isBefore(LocalDateTime.now()) || code.getAttempts() >= MAX_CODE_ATTEMPTS) {
            verificationCodeRepository.delete(code);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE_MESSAGE);
        }
        if (rawCode == null || !passwordEncoder.matches(rawCode, code.getCodeHash())) {
            code.setAttempts(code.getAttempts() + 1);
            if (code.getAttempts() >= MAX_CODE_ATTEMPTS) {
                verificationCodeRepository.delete(code);
            } else {
                verificationCodeRepository.save(code);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE_MESSAGE);
        }
        return code;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE_MESSAGE));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}