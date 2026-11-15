package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.modal.entity.UserPunishment;

import java.util.Collection;
import java.util.List;

public interface UserPunishmentRepository extends JpaRepository<UserPunishment, Long> {

    List<UserPunishment> findByTargetUserIdOrderByAppliedAtDesc(Long targetUserId);

    List<UserPunishment> findByTargetUserIdAndActiveTrueAndTypeIn(Long targetUserId, Collection<String> types);
}
