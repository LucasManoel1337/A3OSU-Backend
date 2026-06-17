package project.service;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.modal.dto.TorneioDTO;
import project.modal.dto.TorneioDetalhesDTO;
import project.modal.dto.TorneioListaDTO;
import project.modal.entity.Torneio;
import project.modal.entity.UserDetalhes;
import project.repository.TorneiosRepository;
import project.repository.UserDetalhesRepository;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TorneiosService {

    private final TorneiosRepository torneioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserDetalhesRepository userDetalhesRepository;

    public TorneiosService(UserService userService, TorneiosRepository torneioRepository, PasswordEncoder passwordEncoder,
                           UserDetalhesRepository userDetalhesRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.torneioRepository = torneioRepository;
        this.userDetalhesRepository = userDetalhesRepository;
    }



    public TorneioDTO criarTorneio(TorneioDTO requestDTO) throws IOException {

        Torneio torneio = new Torneio();
        torneio.setNome(requestDTO.getNome());
        torneio.setTipo(requestDTO.getTipo());
        torneio.setModo(requestDTO.getModo());
        torneio.setVagas(requestDTO.getVagas());
        torneio.setVagasRestantes(requestDTO.getVagas());
        torneio.setDescricao(requestDTO.getDescricao());
        torneio.setIsPrivado(requestDTO.getIsPrivado());
        torneio.setCriadorId(requestDTO.getCriadorId());

        if (Boolean.TRUE.equals(requestDTO.getIsPrivado()) && requestDTO.getSenha() != null && !requestDTO.getSenha().isEmpty()) {
            torneio.setSenha(passwordEncoder.encode(requestDTO.getSenha()));
        }

        if (requestDTO.getBanner() != null && !requestDTO.getBanner().isEmpty()) {
            torneio.setBanner(requestDTO.getBanner().getBytes());
        }

        if (requestDTO.getLogo() != null && !requestDTO.getLogo().isEmpty()) {
            torneio.setLogo(requestDTO.getLogo().getBytes());
        }

        torneio.setDataInicio(requestDTO.getDataInicio());
        torneio.setHoraInicio(requestDTO.getHoraInicio());

        if (requestDTO.getModeradoresIds() != null && !requestDTO.getModeradoresIds().isEmpty()) {
            torneio.setModeradoresIds(requestDTO.getModeradoresIds());
        }

        if ("true".equalsIgnoreCase(requestDTO.getRascunho())) {
            torneio.setStatus("Em Rascunho");
        } else {
            torneio.setStatus("Aguardando Início");
        }

        Torneio torneioSalvo = torneioRepository.save(torneio);
        return mapearParaDTO(torneioSalvo);
    }

    private TorneioDTO mapearParaDTO(Torneio torneio) {
        TorneioDTO dto = new TorneioDTO();
        BeanUtils.copyProperties(torneio, dto);
        return dto;
    }

    public List<TorneioListaDTO> listarTodos() {
        return torneioRepository.findAll().stream()
                .map(this::mapearParaListaDTO)
                .collect(Collectors.toList());
    }

    public TorneioDetalhesDTO buscarDetalhes(Long id) {
        Torneio torneio = torneioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado!"));

        TorneioDetalhesDTO dto = new TorneioDetalhesDTO();
        dto.setId(torneio.getId());
        dto.setNome(torneio.getNome());
        dto.setTipo(torneio.getTipo());
        dto.setModo(torneio.getModo());
        dto.setVagas(torneio.getVagas());
        dto.setVagasRestantes(torneio.getVagasRestantes());
        dto.setDescricao(torneio.getDescricao());
        dto.setIsPrivado(torneio.getIsPrivado());
        dto.setCriadoEm(torneio.getCriadoEm());
        dto.setDataInicio(torneio.getDataInicio());
        dto.setHoraInicio(torneio.getHoraInicio());

        if (torneio.getBanner() != null) {
            String bannerBase64 = Base64.getEncoder().encodeToString(torneio.getBanner());
            dto.setBannerUrlTorneio("data:image/jpeg;base64," + bannerBase64);
        }

        if (torneio.getLogo() != null) {
            String logoBase64 = Base64.getEncoder().encodeToString(torneio.getLogo());
            dto.setLogoUrlTorneio("data:image/jpeg;base64," + logoBase64);
        }

        if (torneio.getCriadorId() != null) {
            UserDetalhes detalhes = userDetalhesRepository.findByUserId(torneio.getCriadorId()).orElse(null);

            if (detalhes != null) {
                dto.setOrganizadorId(String.valueOf(torneio.getCriadorId()));

                // Prioriza o username salvo nos detalhes, se não existir, pega do relacionamento
                String username = detalhes.getUsername() != null ? detalhes.getUsername() :
                        (detalhes.getUser() != null ? detalhes.getUser().getUsername() : "Sistema");

                dto.setOrganizador(username);
                dto.setOrganizadorNacionalidade(detalhes.getNationality() != null ? detalhes.getNationality() : "un");
                dto.setOrganizadorVerificado(String.valueOf(detalhes.getVerificado() != null ? detalhes.getVerificado() : false));

                if (detalhes.getAvatarData() != null) {
                    String avatarBase64 = Base64.getEncoder().encodeToString(detalhes.getAvatarData());
                    dto.setOrganizadorAvatarUrl("data:image/jpeg;base64," + avatarBase64);
                }
            } else {
                definirOrganizadorPadrao(dto);
            }
        } else {
            definirOrganizadorPadrao(dto);
        }

        return dto;
    }

    private void definirOrganizadorPadrao(TorneioDetalhesDTO dto) {
        dto.setOrganizadorId("0");
        dto.setOrganizador("Sistema");
        dto.setOrganizadorNacionalidade("un");
        dto.setOrganizadorVerificado("false");
    }

    private TorneioListaDTO mapearParaListaDTO(Torneio torneio) {
        TorneioListaDTO dto = new TorneioListaDTO();
        dto.setId(torneio.getId());
        dto.setNome(torneio.getNome());
        dto.setTipo(torneio.getTipo());
        dto.setModo(torneio.getModo());
        dto.setVagas(torneio.getVagas());
        dto.setVagasRestantes(torneio.getVagasRestantes());
        dto.setIsPrivado(torneio.getIsPrivado());

        if (torneio.getBanner() != null) {
            dto.setBanner(Base64.getEncoder().encodeToString(torneio.getBanner()));
        }

        if (torneio.getLogo() != null) {
            dto.setLogo(Base64.getEncoder().encodeToString(torneio.getLogo()));
        }

        UserDetalhes userDetalhes = userService.getDetalhesByUserId(torneio.getCriadorId());

        dto.setOrganizadorId(torneio.getCriadorId());
        dto.setOrganizadorUsuario(userDetalhes.getUsername());
        dto.setOrganizadorVerificado(userDetalhes.getVerificado());
        if (userDetalhes.getAvatarData() != null) {
            dto.setOrganizadorAvatar(Base64.getEncoder().encodeToString(userDetalhes.getAvatarData()));
        }

        dto.setDataInicio(torneio.getDataInicio());
        dto.setHoraInicio(torneio.getHoraInicio());

        return dto;
    }
}