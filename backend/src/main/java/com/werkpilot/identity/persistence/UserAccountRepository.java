package com.werkpilot.identity.persistence;

import com.werkpilot.identity.domain.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    Optional<UserAccountEntity> findByEmail(String email);

    @Query("""
            select count(user)
            from UserAccountEntity user
            join user.roles role
            where user.active = true
              and role = :role
              and user.id <> :excludedUserId
            """)
    long countActiveUsersWithRoleExcluding(@Param("role") UserRole role, @Param("excludedUserId") UUID excludedUserId);

    @Query("""
            select distinct user
            from UserAccountEntity user
            join user.roles role
            where user.active = true
              and role = :role
            """)
    List<UserAccountEntity> findActiveUsersWithRole(@Param("role") UserRole role);
}
