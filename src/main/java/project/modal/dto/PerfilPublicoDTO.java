package project.modal.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PerfilPublicoDTO {
    private Long idUser;
    private String username;
    private String nationality;
    private Boolean verificado;
    private LocalDateTime criadoEm;
    private List<String> conquistas;
}