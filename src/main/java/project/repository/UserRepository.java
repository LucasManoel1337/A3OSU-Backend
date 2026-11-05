package project.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.modal.entity.User;
import project.projection.UsuarioBuscaProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Query("SELECT u.id AS id, u.username AS username, d.avatarData AS avatarData, d.nationality AS nacionalidade, d.verificado AS isVerified FROM User u LEFT JOIN u.detalhes d WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<UsuarioBuscaProjection> buscarResumoUsuarios(@Param("termo") String termo);

}