package project.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.modal.entity.Torneio;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorneiosRepository extends JpaRepository<Torneio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Torneio t WHERE t.id = :torneioId")
    Optional<Torneio> findByIdForUpdate(@Param("torneioId") Long torneioId);

    @Query("SELECT t FROM Torneio t JOIN TorneioInscricao i ON t.id = i.torneioId WHERE i.jogadorId = :jogadorId")
    List<Torneio> findTorneiosByJogadorId(@Param("jogadorId") Long jogadorId);

    List<Torneio> findByCriadorId(Long criadorId);

    @Query(value = "SELECT * FROM tb_torneios WHERE FIND_IN_SET(:moderadorId, moderadores_ids) > 0", nativeQuery = true)
    List<Torneio> buscarTorneiosPorModeradorId(@Param("moderadorId") Long moderadorId);
}
