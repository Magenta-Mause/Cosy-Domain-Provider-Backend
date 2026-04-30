package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.Plan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByPasswordResetToken(String passwordResetToken);

    Optional<UserEntity> findByAccessToken(String accessToken);

    Optional<UserEntity> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isVerified = false")
    long countEmailUnverified();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isMfaEnabled = false")
    long countMfaNotEnabled();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.plan = :plan")
    long countByPlan(@Param("plan") Plan plan);
}
