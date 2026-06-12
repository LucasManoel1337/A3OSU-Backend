package project.modal.dto;

import lombok.Data;

@Data
public class UsuarioBuscaDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String nacionalidade;
    private Boolean isVerified;
}