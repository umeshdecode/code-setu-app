package com.cargosetu.push.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.push.dto.PushTokenDTO;
import com.cargosetu.push.dto.PushTokenRegisterDTO;
import com.cargosetu.push.model.UserPushToken;
import com.cargosetu.push.repository.UserPushTokenRepository;
import com.cargosetu.push.util.PrincipalRef;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushTokenServiceImpl implements PushTokenService {

    private final UserPushTokenRepository tokenRepository;

    @Override
    @Transactional
    public GenericDataDTO register(LoggedInUser user, PushTokenRegisterDTO request) {
        GenericDataDTO response = new GenericDataDTO();
        PrincipalRef principal = PrincipalRef.of(user);

        // Same device registering again must upsert, never duplicate (plan §6/§13).
        UserPushToken token = tokenRepository
                .findByPrincipalTypeAndPrincipalIdAndDeviceId(principal.type(), principal.id(), request.getDeviceId())
                .orElseGet(UserPushToken::new);

        token.setPrincipalType(principal.type());
        token.setPrincipalId(principal.id());
        token.setDeviceId(request.getDeviceId());
        token.setPlatform(request.getPlatform());
        token.setDeviceName(request.getDeviceName());
        token.setIsActive(true);
        token.setLastRegisteredAt(LocalDateTime.now());
        token.setLastSeenAt(LocalDateTime.now());

        // A refreshed token string can collide with a stale row for a different
        // account/device that FCM has already recycled - reassign rather than
        // violate the unique constraint, since the old owner's registration is
        // by definition no longer valid for that token value.
        tokenRepository.findByFcmToken(request.getFcmToken())
                .filter(existing -> !existing.getId().equals(token.getId()))
                .ifPresent(existing -> {
                    log.info("Reassigning fcm token from push token record {} to {}:{} device {}",
                            existing.getId(), principal.type(), principal.id(), request.getDeviceId());
                    existing.setIsActive(false);
                    tokenRepository.save(existing);
                });
        token.setFcmToken(request.getFcmToken());

        UserPushToken saved = tokenRepository.save(token);
        log.info("Registered push token id={} for {}:{} platform={}", saved.getId(), principal.type(), principal.id(), saved.getPlatform());

        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Device registered for push notifications.");
        response.setData(toDto(saved));
        return response;
    }

    @Override
    public GenericDataDTO listMine(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        PrincipalRef principal = PrincipalRef.of(user);
        List<PushTokenDTO> tokens = tokenRepository
                .findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(principal.type(), principal.id())
                .stream()
                .map(PushTokenServiceImpl::toDto)
                .toList();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Registered devices fetched successfully!");
        response.setData(tokens);
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO deactivateCurrent(LoggedInUser user, String deviceId) {
        GenericDataDTO response = new GenericDataDTO();
        PrincipalRef principal = PrincipalRef.of(user);
        Optional<UserPushToken> token = tokenRepository
                .findByPrincipalTypeAndPrincipalIdAndDeviceId(principal.type(), principal.id(), deviceId);
        token.ifPresent(t -> {
            t.setIsActive(false);
            tokenRepository.save(t);
        });
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Device unregistered.");
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO deactivateById(LoggedInUser user, Long tokenRecordId) {
        GenericDataDTO response = new GenericDataDTO();
        PrincipalRef principal = PrincipalRef.of(user);
        Optional<UserPushToken> token = tokenRepository
                .findByIdAndPrincipalTypeAndPrincipalId(tokenRecordId, principal.type(), principal.id());
        if (token.isEmpty()) {
            response.setResponseCode(CommonStatusCode.NOT_FOUND);
            response.setResponseMessege("Device not found.");
            return response;
        }
        token.get().setIsActive(false);
        tokenRepository.save(token.get());
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Device removed.");
        return response;
    }

    @Override
    @Transactional
    public void deactivateByFcmToken(String fcmToken) {
        tokenRepository.findByFcmToken(fcmToken).ifPresent(token -> {
            token.setIsActive(false);
            tokenRepository.save(token);
            log.info("Deactivated push token id={} - provider reported it unregistered", token.getId());
        });
    }

    private static PushTokenDTO toDto(UserPushToken token) {
        PushTokenDTO dto = new PushTokenDTO();
        dto.setId(token.getId());
        dto.setPlatform(token.getPlatform());
        dto.setDeviceName(token.getDeviceName());
        dto.setLastSeenAt(token.getLastSeenAt());
        dto.setLastRegisteredAt(token.getLastRegisteredAt());
        return dto;
    }
}
