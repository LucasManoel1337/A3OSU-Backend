package project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.modal.request.CadastroRequest;
import project.modal.request.ForgotPasswordRequest;
import project.modal.request.LoginRequest;
import project.modal.request.ResetPasswordRequest;
import project.modal.request.ResendVerificationRequest;
import project.modal.request.VerifyEmailRequest;
import project.service.AuthService;
import project.service.AuthSessionResult;
import project.modal.response.AuthResponse;
import project.modal.response.MessageResponse;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);

    @Value("${api.cors.allowed-origin}")
    private String allowedOrigin;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<MessageResponse> cadastrar(@RequestBody CadastroRequest request) {
        MessageResponse response = authService.registrar(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthSessionResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin) {
        requireFrontendOrigin(origin);
        AuthSessionResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin) {
        requireFrontendOrigin(origin);
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<MessageResponse> verificarEmail(@RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verificarEmail(request));
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<MessageResponse> reenviarVerificacao(@RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.reenviarVerificacao(request));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<MessageResponse> esqueciSenha(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.solicitarRedefinicaoSenha(request));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<MessageResponse> redefinirSenha(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.redefinirSenha(request));
    }

    private ResponseCookie refreshCookie(String value) {
        boolean secure = frontendUsesHttps();
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/api/auth")
                .maxAge(SESSION_TIMEOUT)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        boolean secure = frontendUsesHttps();
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private boolean frontendUsesHttps() {
        return allowedOrigin.trim().regionMatches(true, 0, "https://", 0, 8);
    }

    private void requireFrontendOrigin(String origin) {
        if (origin == null || !allowedOrigin.trim().equals(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem não permitida.");
        }
    }
}