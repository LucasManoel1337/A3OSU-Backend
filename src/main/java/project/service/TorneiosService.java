package project.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.modal.dto.*;
import project.modal.entity.Torneio;
import project.modal.entity.TorneioInscricao;
import project.modal.entity.UserDetalhes;
import project.repository.InscricaoTorneioRepository;
import project.repository.TorneiosRepository;
import project.repository.UserDetalhesRepository;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TorneiosService {

    private final TorneiosRepository torneioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserDetalhesRepository userDetalhesRepository;
    private final InscricaoTorneioRepository inscricaoRepository;

    public TorneiosService(UserService userService, TorneiosRepository torneioRepository, PasswordEncoder passwordEncoder,
                           UserDetalhesRepository userDetalhesRepository, InscricaoTorneioRepository inscricaoRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.torneioRepository = torneioRepository;
        this.userDetalhesRepository = userDetalhesRepository;
        this.inscricaoRepository = inscricaoRepository;
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

    public List<TorneioListaDTO> listarTodosAtivos() {
        return torneioRepository.findAll().stream()
                .filter(t -> "Aguardando Início".equals(t.getStatus()) || "Em Andamento".equals(t.getStatus()))
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

        dto.setStatus(torneio.getStatus());

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
        dto.setStatus(torneio.getStatus());

        return dto;
    }

    @Transactional
    public void entrarNoTorneio(Long torneioId, EntrarTorneioDTO request) {

        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado."));

        if ("Em Rascunho".equalsIgnoreCase(torneio.getStatus())) {
            throw new RuntimeException("Este torneio ainda não está aberto para inscrições.");
        }

        if (torneio.getVagasRestantes() <= 0) {
            throw new RuntimeException("O torneio já está lotado.");
        }

        if (inscricaoRepository.existsByTorneioIdAndJogadorId(torneioId, request.getJogadorId())) {
            throw new RuntimeException("Você já está participando deste torneio.");
        }

        if (Boolean.TRUE.equals(torneio.getIsPrivado())) {
            if (request.getSenha() == null || !passwordEncoder.matches(request.getSenha(), torneio.getSenha())) {
                throw new RuntimeException("Senha incorreta para este torneio privado.");
            }
        }

        TorneioInscricao inscricao = new TorneioInscricao();
        inscricao.setTorneioId(torneio.getId());
        inscricao.setJogadorId(request.getJogadorId());
        inscricaoRepository.save(inscricao);

        torneio.setVagasRestantes(torneio.getVagasRestantes() - 1);
        torneioRepository.save(torneio);

        UserDetalhes detalhes = userDetalhesRepository.findByUserId(request.getJogadorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        if (!detalhes.getConquistas().contains("PRIMEIRA_BATALHA")) {
            detalhes.getConquistas().add("PRIMEIRA_BATALHA");
            userDetalhesRepository.save(detalhes);
        }
    }

    public List<InscritoDTO> buscarInscritosDoTorneio(Long torneioId) {
        List<TorneioInscricao> inscricoes = inscricaoRepository.findByTorneioIdOrderByPontuacaoDescDataInscricaoAsc(torneioId);

        return inscricoes.stream().map(inscricao -> {

            var detalhes = userDetalhesRepository.findByUserId(inscricao.getJogadorId())
                    .orElseThrow(() -> new RuntimeException("Detalhes do usuário não encontrados para ID: " + inscricao.getJogadorId()));

            String avatarBase64 = null;
            if (detalhes.getAvatarData() != null && detalhes.getAvatarData().length > 0) {
                avatarBase64 = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(detalhes.getAvatarData());
            }

            return new InscritoDTO(
                    inscricao.getJogadorId(),
                    detalhes.getUsername(),
                    detalhes.getNationality(),
                    inscricao.getPontuacao(),
                    avatarBase64,
                    detalhes.getVerificado()
            );
        }).toList();
    }

    public List<TorneioListaDTO> listarParticipativos(Long jogadorId) {
        return torneioRepository.findTorneiosByJogadorId(jogadorId).stream()
                .map(this::mapearParaListaDTO)
                .collect(Collectors.toList());
    }

    public List<TorneioListaDTO> listarCriados(Long criadorId) {
        return torneioRepository.findByCriadorId(criadorId).stream()
                .map(this::mapearParaListaDTO)
                .collect(Collectors.toList());
    }

    public List<TorneioListaDTO> listarModerando(Long moderadorId) {
        return torneioRepository.buscarTorneiosPorModeradorId(moderadorId).stream()
                .map(this::mapearParaListaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adicionarModerador(Long torneioId, Long moderadorId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado."));

        if (!torneio.getModeradoresIds().contains(moderadorId)) {
            torneio.getModeradoresIds().add(moderadorId);
            torneioRepository.save(torneio);
        }
    }

    @Transactional
    public void removerModerador(Long torneioId, Long moderadorId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado."));

        torneio.getModeradoresIds().remove(moderadorId);
        torneioRepository.save(torneio);
    }

    public List<InscritoDTO> buscarModeradores(Long torneioId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado."));

        List<Long> modIds = torneio.getModeradoresIds();
        if (modIds == null || modIds.isEmpty()) {
            return List.of();
        }

        return modIds.stream().map(id -> {
            var detalhesOpt = userDetalhesRepository.findByUserId(id);
            if (detalhesOpt.isPresent()) {
                var det = detalhesOpt.get();
                String avatarBase64 = null;
                if (det.getAvatarData() != null && det.getAvatarData().length > 0) {
                    avatarBase64 = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(det.getAvatarData());
                }
                return new InscritoDTO(
                        id, det.getUsername(), det.getNationality(),
                        (int) 0.0, avatarBase64, det.getVerificado()
                );
            }
            return null;
        }).filter(java.util.Objects::nonNull).toList();
    }

    public void atualizarStatus(Long torneioId, String novoStatus, String usuarioLogadoId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado"));

        UserDetalhes user = userDetalhesRepository.findByUsername(usuarioLogadoId);

        // Segurança: Validação 403 (Só o criador pode mudar o status)
        if (!torneio.getCriadorId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar este torneio.");
        }

        torneio.setStatus(novoStatus);
        torneioRepository.save(torneio);
    }

    public void atualizarPontuacao(Long torneioId, Long jogadorId, Integer pontuacao, String usuarioLogadoId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RuntimeException("Torneio não encontrado"));

        TorneioInscricao inscrito = inscricaoRepository.findByTorneioIdAndJogadorId(torneioId, jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado neste torneio"));

        inscrito.setPontuacao(pontuacao);
        inscricaoRepository.save(inscrito);
    }
}