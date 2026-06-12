package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project.modal.dto.TorneioDTO;
import project.modal.entity.Torneio;
import project.repository.TorneiosRepository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TorneiosService {

    private final TorneiosRepository torneioRepository;
    private final PasswordEncoder passwordEncoder;

    public TorneioDTO criarTorneio(
            Long criadorId,
            String nome, String tipo, String modo, Integer vagas,
            String descricao, Boolean isPrivado, String senha,
            MultipartFile banner, MultipartFile logo) throws IOException { // <-- Lança IOException pelos bytes

        Torneio torneio = new Torneio();
        torneio.setNome(nome);
        torneio.setTipo(tipo);
        torneio.setModo(modo);
        torneio.setVagas(vagas);
        torneio.setDescricao(descricao);
        torneio.setIsPrivado(isPrivado);
        torneio.setCriadorId(criadorId);

        if (Boolean.TRUE.equals(isPrivado) && senha != null && !senha.isEmpty()) {
            torneio.setSenha(passwordEncoder.encode(senha));
        }

        // Salva os bytes do Banner no Banco
        if (banner != null && !banner.isEmpty()) {
            torneio.setBanner(banner.getBytes());
        }

        // Salva os bytes da Logo no Banco
        if (logo != null && !logo.isEmpty()) {
            torneio.setLogo(logo.getBytes());
        }

        Torneio torneioSalvo = torneioRepository.save(torneio);
        return mapearParaDTO(torneioSalvo);
    }

    private TorneioDTO mapearParaDTO(Torneio torneio) {
        TorneioDTO dto = new TorneioDTO();
        BeanUtils.copyProperties(torneio, dto);
        return dto;
    }

    public List<TorneioDTO> listarTodos() {
        return torneioRepository.findAll().stream()
                .map(this::mapearParaDTO)
                .collect(Collectors.toList());
    }
}