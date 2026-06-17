package project.modal.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TorneioDTO {
    private Long id;
    private String nome;
    private String tipo;
    private String modo;
    private Integer vagas;
    private Integer vagasRestantes;
    private String descricao;
    private Boolean isPrivado;
    private MultipartFile banner;
    private MultipartFile logo;
    private Long criadorId;
    private String senha;
}
