package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.modal.entity.AuthVerificationCode;
import project.modal.entity.VerificationPurpose;

import java.util.Optional;

@Repository
public interface AuthVerificationCodeRepository extends JpaRepository<AuthVerificationCode, Long> {

    Optional<AuthVerificationCode> findByUser_IdAndPurpose(Long userId, VerificationPurpose purpose);

    void deleteByUser_IdAndPurpose(Long userId, VerificationPurpose purpose);
}
