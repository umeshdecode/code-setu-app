package com.cargosetu.push.repository;

import com.cargosetu.push.model.PrincipalType;
import com.cargosetu.push.model.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    List<UserPushToken> findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(PrincipalType principalType, Long principalId);

    /** Upsert key: same account + same installation must update, never duplicate. */
    Optional<UserPushToken> findByPrincipalTypeAndPrincipalIdAndDeviceId(PrincipalType principalType, Long principalId, String deviceId);

    Optional<UserPushToken> findByFcmToken(String fcmToken);

    /** Ownership check: a caller may only ever touch their own token record. */
    Optional<UserPushToken> findByIdAndPrincipalTypeAndPrincipalId(Long id, PrincipalType principalType, Long principalId);
}
