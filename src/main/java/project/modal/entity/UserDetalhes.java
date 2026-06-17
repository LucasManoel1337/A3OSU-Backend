package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    private String nationality;
    private String language;
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_users_conquistas", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "conquista_codigo")
    private List<String> conquistas = new ArrayList<>();
}
