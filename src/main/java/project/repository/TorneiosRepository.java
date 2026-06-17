package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.modal.entity.Torneio;

import java.util.List;

@Repository
public interface TorneiosRepository extends JpaRepository<Torneio, Long> {

    @Query("SELECT t FROM Torneio t JOIN TorneioInscricao i ON t.id = i.torneioId WHERE i.jogadorId = :jogadorId")
    List<Torneio> findTorneiosByJogadorId(@Param("jogadorId") Long jogadorId);

    List<Torneio> findByCriadorId(Long criadorId);

    @Query(value = "SELECT * FROM tb_torneios WHERE FIND_IN_SET(:moderadorId, moderadores_ids) > 0", nativeQuery = true)
    List<Torneio> buscarTorneiosPorModeradorId(@Param("moderadorId") Long moderadorId);
}
