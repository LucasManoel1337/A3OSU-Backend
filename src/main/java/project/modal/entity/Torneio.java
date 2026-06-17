package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

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