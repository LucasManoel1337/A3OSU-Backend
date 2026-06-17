package project.service;

import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.modal.response.AuthResponse;
import project.modal.entity.User;
import project.modal.entity.UserDetalhes;
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

    @Transactional // Abre uma transação única para salvar nas duas tabelas com segurança
    public AuthResponse registrar(CadastroRequest request) {

        // 1. Verifica se o usuário ou email já existem
        if (userRepository.existsByUsername(request.username()) ||
                userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Usuário ou e-mail já cadastrado!");
        }

        // 2. Cria a entidade principal do Usuário
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        // 3. Cria a nova entidade de Detalhes (Substituindo o antigo UserConfig)
        UserDetalhes detalhes = new UserDetalhes();
        detalhes.setNationality(request.nationality());
        detalhes.setLanguage(request.language());
        detalhes.setUsername(request.username());
        detalhes.setVerificado(false); // Garante o padrão não verificado no ato do cadastro
        detalhes.setUser(user); // Amarra a chave estrangeira ao usuário acima

        // === LIGAR A MEDALHA DE MEMBRO BETA AQUI ===
        detalhes.getConquistas().add("MEMBRO_BETA");

        // 4. Vincula os detalhes dentro do objeto do usuário
        user.setDetalhes(detalhes);

        // 5. Salva o usuário no banco (O Cascade salva User, UserDetalhes e a lista de Conquistas!)
        userRepository.save(user);

        // 6. Gera o token e devolve o AuthResponse original para o controller
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