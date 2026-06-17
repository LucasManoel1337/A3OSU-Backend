package project.modal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InscritoDTO {
    private Long id; // ID do jogador (para clicar e ir pro perfil)
    private String username;
    private String nacionalidade;
    private Integer pontuacao;
    private String avatarUrl;
    private Boolean isVerified;
}