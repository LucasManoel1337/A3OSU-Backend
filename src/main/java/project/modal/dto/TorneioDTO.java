package project.modal.dto;

import lombok.Data;

@Data
public class TorneioDTO {
    private Long id;
    private String nome;
    private String tipo;
    private String modo;
    private Integer vagas;
    private String descricao;
    private Boolean isPrivado;
    private byte[] banner;
    private byte[] logo;
    private Long criadorId;
}
