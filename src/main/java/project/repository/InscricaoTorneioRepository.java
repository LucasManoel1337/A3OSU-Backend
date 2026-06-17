package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.modal.entity.TorneioInscricao;

import java.util.List;

@Repository
public interface InscricaoTorneioRepository extends JpaRepository<TorneioInscricao, Long> {

    boolean existsByTorneioIdAndJogadorId(Long torneioId, Long jogadorId);

    List<TorneioInscricao> findByTorneioIdOrderByPontuacaoDescDataInscricaoAsc(Long torneioId);
}