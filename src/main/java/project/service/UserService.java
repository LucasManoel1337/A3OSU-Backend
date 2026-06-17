package project.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.modal.dto.*;
import project.modal.entity.User;
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

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final UserDetalhesRepository userDetalhesrepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserDetalhesRepository userDetalhesrepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetalhesrepository = userDetalhesrepository;
    }

    @Transactional
    public void updateProfile(String username, UpdateProfileDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserDetalhes detalhes = userDetalhesrepository.findByUserId(user.getId())
                .orElse(new UserDetalhes());

        if (detalhes.getId() == null) {
            detalhes.setUser(user);
            detalhes.setVerificado(false);
        }

        detalhes.setNationality(dto.getNationality());
        detalhes.setLanguage(dto.getLanguage());

        userDetalhesrepository.save(detalhes);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("A senha atual está incorreta!");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("A nova senha e a confirmação não coincidem!");
        }

        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$";
        if (!dto.getNewPassword().matches(regex)) {
            throw new IllegalArgumentException("A nova senha não cumpre os requisitos de segurança!");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Optional<UserDetalhes> userDet = userDetalhesrepository.findByUserId(user.getId());

        String nationality = userDet.map(UserDetalhes::getNationality).orElse("");
        String language = userDet.map(UserDetalhes::getLanguage).orElse("");
        Boolean verificado = userDet.map(UserDetalhes::getVerificado).orElse(false);

        List<String> conquistas = userDet.map(UserDetalhes::getConquistas).orElse(List.of());

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                nationality,
                language,
                verificado,
                conquistas
        );
    }

    public void updateAsset(Long userId, MultipartFile file, String type) throws IOException {
        if (file.getSize() > MAX_SIZE) throw new RuntimeException("Arquivo excede 5MB");

        UserDetalhes detalhes = userDetalhesrepository.findByUserId(userId)
                .orElse(new UserDetalhes());

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
        List<UsuarioBuscaProjection> projecoes = userRepository.buscarResumoUsuarios(termo);

        return projecoes.stream()
                .map(this::mapearParaBuscaDTO)
                .collect(Collectors.toList());
    }

    private UsuarioBuscaDTO mapearParaBuscaDTO(UsuarioBuscaProjection proj) {
        UsuarioBuscaDTO dto = new UsuarioBuscaDTO();
        dto.setId(proj.getId());
        dto.setUsername(proj.getUsername());

        dto.setNacionalidade(proj.getNacionalidade() != null ? proj.getNacionalidade() : "un");
        dto.setIsVerified(proj.getIsVerified() != null ? proj.getIsVerified() : false);

        if (proj.getAvatarData() != null) {
            String base64 = Base64.getEncoder().encodeToString(proj.getAvatarData());
            dto.setAvatarUrl("data:image/jpeg;base64," + base64);
        } else {
            dto.setAvatarUrl(null);
        }

        return dto;
    }

    public PerfilPublicoDTO buscarPerfilPublico(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        PerfilPublicoDTO dto = new PerfilPublicoDTO();
        dto.setIdUser(user.getId());
        dto.setUsername(user.getUsername());
        dto.setCriadoEm(user.getCreatedAt());

        if (user.getDetalhes() != null) {
            dto.setNationality(user.getDetalhes().getNationality() != null ? user.getDetalhes().getNationality() : "un");
            dto.setVerificado(user.getDetalhes().getVerificado() != null ? user.getDetalhes().getVerificado() : false);
        } else {
            dto.setNationality("un");
            dto.setVerificado(false);
        }

        return dto;
    }
}