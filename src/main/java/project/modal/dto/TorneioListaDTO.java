package project.modal.dto;

import lombok.Data;

@Data
public class TorneioListaDTO {
    private Long id;
    private String nome;
    private String tipo;
    private String modo;
    private Integer vagas;
    private Integer vagasRestantes;
    private Boolean isPrivado;
    private String banner;
    private String logo;
    private Long organizadorId;
    private String organizadorUsuario;
    private Boolean organizadorVerificado;
    private String organizadorAvatar;
}