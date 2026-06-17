package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_torneios")
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String modo;

    @Column(nullable = false)
    private Integer vagas;

    @Column(nullable = false)
    private Integer vagasRestantes;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "is_privado", nullable = false)
    private Boolean isPrivado;

    private String senha;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] banner;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] logo;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    private Long criadorId;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}