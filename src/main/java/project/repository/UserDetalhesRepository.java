package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.modal.entity.UserDetalhes;

import java.util.Optional;

@Repository
public interface UserDetalhesRepository extends JpaRepository<UserDetalhes, Long> {
    Optional<UserDetalhes> findByUserId(Long userId);

    UserDetalhes findByUsername(String usuarioLogadoId);
}