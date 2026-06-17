package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_inscricao_torneio")
public class TorneioInscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "torneio_id", nullable = false)
    private Long torneioId;

    @Column(name = "jogador_id", nullable = false)
    private Long jogadorId;

    @Column(name = "pontuacao")
    private Integer pontuacao;

    @Column(name = "data_inscricao", nullable = false)
    private LocalDateTime dataInscricao;

    @PrePersist
    protected void onCreate() {
        this.pontuacao = 0;
        this.dataInscricao = LocalDateTime.now();
    }
}