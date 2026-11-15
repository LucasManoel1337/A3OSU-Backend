package project.modal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_user_punishments")
public class UserPunishment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applied_by_user_id", nullable = false)
    private User appliedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ended_by_user_id")
    private User endedBy;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
