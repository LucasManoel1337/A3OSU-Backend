package project.modal.entity;

import jakarta.persistence.*;
import lombok.Data;

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
}
