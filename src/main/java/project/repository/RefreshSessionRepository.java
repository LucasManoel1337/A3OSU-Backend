package project.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.modal.entity.RefreshSession;

import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RefreshSession s JOIN FETCH s.user WHERE s.refreshTokenHash = :tokenHash")
    Optional<RefreshSession> findByRefreshTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
