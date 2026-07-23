package com.swimteam.repository;

import com.swimteam.domain.MemberProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
    Optional<MemberProfile> findByUserId(Long userId);

    @Query("select p from MemberProfile p join fetch p.user u where u.role = com.swimteam.domain.Role.MEMBER")
    List<MemberProfile> findAllMemberProfiles();

    long countByProfileCompletedTrue();
}
