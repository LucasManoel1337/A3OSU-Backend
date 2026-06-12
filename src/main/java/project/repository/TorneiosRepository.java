package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.modal.entity.Torneio;

@Repository
public interface TorneiosRepository extends JpaRepository<Torneio, Long> {
}
