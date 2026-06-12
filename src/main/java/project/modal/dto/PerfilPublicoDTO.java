package project.modal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PerfilPublicoDTO {
    private Long idUser;
    private String username;
    private String nationality;
    private Boolean verificado;
    private LocalDateTime criadoEm; // Para mostrarmos a data real de entrada!
}