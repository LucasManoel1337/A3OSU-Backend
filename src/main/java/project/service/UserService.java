package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.modal.dto.*;
import project.modal.entity.User;
import project.modal.entity.UserConfig;
import project.modal.entity.UserDetalhes;
import project.projection.UsuarioBuscaProjection;
import project.repository.UserDetalhesRepository;
import project.repository.UserRepository;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private UserDetalhesRepository userDetalhesrepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserDetalhesRepository userDetalhesrepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetalhesrepository = userDetalhesrepository;
    }

    // 1. ATUALIZAR NACIONALIDADE E IDIOMA
    @Transactional
    public void updateProfile(String username, UpdateProfileDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserConfig config = user.getUserConfig();

        // Se por algum motivo o usuário não tiver a linha de config, criamos uma nova
        if (config == null) {
            config = new UserConfig();
            config.setUser(user);
            user.setUserConfig(config);
        }

        config.setNationality(dto.getNationality());
        config.setLanguage(dto.getLanguage());

        userRepository.save(user); // O Cascade salva a tabela tb_users_config automaticamente
    }

    // 2. ALTERAR A SENHA
    @Transactional
    public void changePassword(String username, ChangePasswordDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Validação A: A senha atual digitada bate com a senha criptografada do banco?
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("A senha atual está incorreta!");
        }

        // Validação B: A nova senha e a confirmação são iguais?
        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("A nova senha e a confirmação não coincidem!");
        }

        // Validação C: A nova senha cumpre os requisitos do Regex?
        // (Maiúscula, minúscula, caractere especial, número, entre 6 e 20 dígitos)
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$";
        if (!dto.getNewPassword().matches(regex)) {
            throw new IllegalArgumentException("A nova senha não cumpre os requisitos de segurança!");
        }

        // Se passou em tudo, criptografa a nova senha e salva
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String nationality = user.getUserConfig() != null ? user.getUserConfig().getNationality() : "";
        String language = user.getUserConfig() != null ? user.getUserConfig().getLanguage() : "";

        Optional<UserDetalhes> userDet = userDetalhesrepository.findByUserId(user.getId());

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                nationality,
                language,
                userDet.get().getVerificado()
        );
    }

    public void updateAsset(Long userId, MultipartFile file, String type) throws IOException {
        if (file.getSize() > MAX_SIZE) throw new RuntimeException("Arquivo excede 5MB");

        UserDetalhes detalhes = userDetalhesrepository.findByUserId(userId)
                .orElse(new UserDetalhes());

        // IMPORTANTE: Se for um registro novo, precisamos associar o usuário à entidade
        if (detalhes.getId() == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            detalhes.setUser(user);
            detalhes.setVerificado(false);
        }

        if ("avatar".equals(type)) detalhes.setAvatarData(file.getBytes());
        else if ("banner".equals(type)) detalhes.setBannerData(file.getBytes());

        userDetalhesrepository.save(detalhes);
    }

    public UserDetalhes getDetalhesByUserId(Long userId) {
        return userDetalhesrepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Detalhes não encontrados"));
    }

    public List<UsuarioBuscaDTO> buscarUsuariosPorNick(String termo) {
        // Traz apenas as colunas solicitadas, blindando dados sensíveis
        List<UsuarioBuscaProjection> projecoes = userRepository.buscarResumoUsuarios(termo);

        return projecoes.stream()
                .map(this::mapearParaBuscaDTO)
                .collect(Collectors.toList());
    }

    private UsuarioBuscaDTO mapearParaBuscaDTO(UsuarioBuscaProjection proj) {
        UsuarioBuscaDTO dto = new UsuarioBuscaDTO();
        dto.setId(proj.getId());
        dto.setUsername(proj.getUsername());

        // Nacionalidade (com fallback de segurança)
        dto.setNacionalidade(proj.getNacionalidade() != null ? proj.getNacionalidade() : "un");

        // Verificado (com fallback de segurança)
        dto.setIsVerified(proj.getIsVerified() != null ? proj.getIsVerified() : false);

        // Converte a imagem
        if (proj.getAvatarData() != null) {
            String base64 = Base64.getEncoder().encodeToString(proj.getAvatarData());
            dto.setAvatarUrl("data:image/jpeg;base64," + base64);
        } else {
            dto.setAvatarUrl(null);
        }

        return dto;
    }

    public PerfilPublicoDTO buscarPerfilPublico(Long id) {
        // Agora usamos findById nativo do JPA
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        PerfilPublicoDTO dto = new PerfilPublicoDTO();
        dto.setIdUser(user.getId());
        dto.setUsername(user.getUsername());
        dto.setCriadoEm(user.getCreatedAt());

        // Nacionalidade
        if (user.getConfig() != null && user.getConfig().getNationality() != null) {
            dto.setNationality(user.getConfig().getNationality());
        } else {
            dto.setNationality("un");
        }

        // Verificado
        if (user.getDetalhes() != null && user.getDetalhes().getVerificado() != null) {
            dto.setVerificado(user.getDetalhes().getVerificado());
        } else {
            dto.setVerificado(false);
        }

        return dto;
    }
}