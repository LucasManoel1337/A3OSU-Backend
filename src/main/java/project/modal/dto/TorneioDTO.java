package project.modal.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    private LocalDate dataInicio;
    private LocalTime horaInicio;
    private List<Long> moderadoresIds;
    private String rascunho;
}
