package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "tb_users_detalhes")
public class UserDetalhes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] avatarData;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] bannerData;

    @Column(name = "verificado")
    private Boolean verificado;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    @Column(name = "titulo_selecionado", length = 60)
    private String tituloSelecionado;

    private String nationality;
    private String language;
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_users_conquistas", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "conquista_codigo")
    private List<String> conquistas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_users_titulos", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "titulo", length = 60)
    private List<String> titulos = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime CriadoEm;
}
