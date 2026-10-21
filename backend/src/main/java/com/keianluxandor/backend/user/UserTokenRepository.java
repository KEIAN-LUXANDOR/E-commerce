package com.keianluxandor.backend.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

    // Locks the row so the same code can't be used twice at the same moment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserToken> findByTokenHashAndType(String tokenHash, TokenType type);

    // Cancels all older unused codes of this type for this user
    @Modifying
    @Query("update UserToken t set t.usedAt = :now "
            + "where t.user = :user and t.type = :type and t.usedAt is null")
    int invalidateAll(@Param("user") User user,
                      @Param("type") TokenType type,
                      @Param("now") Instant now);
}