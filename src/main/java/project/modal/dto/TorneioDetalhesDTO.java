package project.modal.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TorneioDetalhesDTO {
    private Long id;
    private String nome;
    private String tipo;
    private String modo;
    private Integer vagas;
    private Integer vagasRestantes;
    private String descricao;
    private Boolean isPrivado;
    private String bannerUrlTorneio;
    private String logoUrlTorneio;
    private String organizadorId;
    private String organizador;
    private String organizadorNacionalidade;
    private String organizadorVerificado;
    private String organizadorAvatarUrl;
    private LocalDateTime criadoEm;
}