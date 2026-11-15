package project.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.*;

@Service
public class UserService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> CONQUISTAS_CONCEDIVEIS =
            Set.of("FOUNDER", "MEMBRO_BETA", "PRIMEIRA_BATALHA", "ORGANIZADOR");
    private static final String TITULO_COMPETIDOR_CASUAL = "Competidor casual";
    private static final String TITULO_ORGANIZADOR = "Organizador de Torneios";
    private static final String TITULO_FUNDADOR = "Fundador da plataforma";

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
        LocalDateTime dataCriacao = userDet.map(UserDetalhes::getCriadoEm).orElse(null);

        List<String> conquistas = userDet.map(UserDetalhes::getConquistas).orElse(List.of());
        List<String> titulosDisponiveis = userDet.map(this::titulosDisponiveis).orElse(List.of());
        String tituloSelecionado = userDet.map(UserDetalhes::getTituloSelecionado)
                .filter(titulosDisponiveis::contains)
                .orElse(null);

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                nationality,
                language,
                verificado,
                conquistas,
                titulosDisponiveis,
                tituloSelecionado,
                dataCriacao
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

    @Transactional(readOnly = true)
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
            dto.setConquistas(user.getDetalhes().getConquistas() != null ? user.getDetalhes().getConquistas() : List.of());
            List<String> titulos = titulosDisponiveis(user.getDetalhes());
            String tituloSelecionado = user.getDetalhes().getTituloSelecionado();
            if (tituloSelecionado != null && titulos.contains(tituloSelecionado)) {
                dto.setTituloSelecionado(tituloSelecionado);
            }
        } else {
            dto.setNationality("un");
            dto.setVerificado(false);
            dto.setConquistas(List.of());
        }

        return dto;
    }

    @Transactional
    public void selecionarTitulo(String username, String titulo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        UserDetalhes detalhes = obterOuCriarDetalhes(user);
        String tituloNormalizado = titulo == null || titulo.isBlank() ? null : titulo.trim();

        if (tituloNormalizado != null && !titulosDisponiveis(detalhes).contains(tituloNormalizado)) {
            throw new IllegalArgumentException("Esse título ainda não foi desbloqueado.");
        }

        detalhes.setTituloSelecionado(tituloNormalizado);
        userDetalhesrepository.save(detalhes);
    }

    @Transactional(readOnly = true)
    public List<ModeracaoUsuarioDTO> pesquisarUsuariosParaModeracao(String usernameFundador, String termo) {
        exigirFundador(usernameFundador);
        String busca = termo == null ? "" : termo.trim();
        if (busca.length() < 2) {
            return List.of();
        }

        return userRepository.findTop25ByUsernameContainingIgnoreCaseOrderByUsernameAsc(busca).stream()
                .map(user -> {
                    UserDetalhes detalhes = user.getDetalhes();
                    return new ModeracaoUsuarioDTO(
                            user.getId(),
                            user.getUsername(),
                            detalhes == null || detalhes.getAvatarData() == null
                                    ? null
                                    : "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(detalhes.getAvatarData()),
                            detalhes == null || detalhes.getNationality() == null ? "un" : detalhes.getNationality(),
                            detalhes != null && Boolean.TRUE.equals(detalhes.getVerificado()),
                            detalhes == null ? List.of() : List.copyOf(detalhes.getConquistas()),
                            detalhes == null ? List.of() : List.copyOf(detalhes.getTitulos())
                    );
                })
                .toList();
    }

    @Transactional
    public void atualizarVerificado(String usernameFundador, Long userId, Boolean verificado) {
        exigirFundador(usernameFundador);
        if (verificado == null) {
            throw new IllegalArgumentException("Informe o novo status de verificação.");
        }
        User user = usuarioAlvo(userId);
        UserDetalhes detalhes = obterOuCriarDetalhes(user);
        detalhes.setVerificado(verificado);
        userDetalhesrepository.save(detalhes);
    }

    @Transactional
    public void atualizarConquista(String usernameFundador, Long userId, String codigo, Boolean conceder) {
        exigirFundador(usernameFundador);
        if (codigo == null || !CONQUISTAS_CONCEDIVEIS.contains(codigo) || conceder == null) {
            throw new IllegalArgumentException("Conquista ou ação inválida.");
        }
        UserDetalhes detalhes = obterOuCriarDetalhes(usuarioAlvo(userId));
        if (conceder && !detalhes.getConquistas().contains(codigo)) {
            detalhes.getConquistas().add(codigo);
        } else if (!conceder) {
            detalhes.getConquistas().removeIf(codigo::equals);
        }
        userDetalhesrepository.save(detalhes);
    }

    @Transactional
    public void atualizarTitulo(String usernameFundador, Long userId, String titulo, Boolean conceder) {
        exigirFundador(usernameFundador);
        String tituloNormalizado = titulo == null ? "" : titulo.trim();
        if (tituloNormalizado.isEmpty() || tituloNormalizado.length() > 60
                || tituloNormalizado.chars().anyMatch(Character::isISOControl) || conceder == null) {
            throw new IllegalArgumentException("O título deve ter entre 1 e 60 caracteres.");
        }

        UserDetalhes detalhes = obterOuCriarDetalhes(usuarioAlvo(userId));
        if (conceder && !detalhes.getTitulos().contains(tituloNormalizado)) {
            detalhes.getTitulos().add(tituloNormalizado);
        } else if (!conceder) {
            detalhes.getTitulos().removeIf(tituloNormalizado::equals);
            if (tituloNormalizado.equals(detalhes.getTituloSelecionado())) {
                detalhes.setTituloSelecionado(null);
            }
        }
        userDetalhesrepository.save(detalhes);
    }

    private void exigirFundador(String username) {
        User fundador = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Acesso restrito a fundadores."));
        UserDetalhes detalhes = fundador.getDetalhes();
        if (detalhes == null || !detalhes.getConquistas().contains("FOUNDER")) {
            throw new AccessDeniedException("Acesso restrito a fundadores.");
        }
    }

    private User usuarioAlvo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private UserDetalhes obterOuCriarDetalhes(User user) {
        return userDetalhesrepository.findByUserId(user.getId()).orElseGet(() -> {
            UserDetalhes detalhes = new UserDetalhes();
            detalhes.setUser(user);
            detalhes.setUsername(user.getUsername());
            detalhes.setVerificado(false);
            detalhes.setEmailVerificado(false);
            detalhes.setCriadoEm(LocalDateTime.now());
            return detalhes;
        });
    }

    private List<String> titulosDisponiveis(UserDetalhes detalhes) {
        LinkedHashSet<String> titulos = new LinkedHashSet<>(detalhes.getTitulos());
        List<String> conquistas = detalhes.getConquistas();
        if (conquistas.contains("PRIMEIRA_BATALHA")) {
            titulos.add(TITULO_COMPETIDOR_CASUAL);
        }
        if (conquistas.contains("ORGANIZADOR")) {
            titulos.add(TITULO_ORGANIZADOR);
        }
        if (conquistas.contains("FOUNDER")) {
            titulos.add(TITULO_FUNDADOR);
        }
        return List.copyOf(titulos);
    }
}