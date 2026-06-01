package project.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.modal.response.AuthResponse;
import project.modal.entity.User;
import project.repository.UserRepository;
import project.modal.request.CadastroRequest;
import project.modal.request.LoginRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse registrar(CadastroRequest request) {
        // Verifica se o usuário ou email já existem
        if (userRepository.existsByUsername(request.username()) ||
                userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Usuário ou e-mail já cadastrado!");
        }

        // Cria a entidade e encripta a senha
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        // Opcional: Já logar o usuário logo após o cadastro e devolver o token
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        // O AuthenticationManager verifica se a senha bate com o banco
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Se passar da linha acima, as credenciais estão corretas
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername());
    }
}