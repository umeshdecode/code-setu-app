package com.cargosetu.push;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.push.dto.PushTokenRegisterDTO;
import com.cargosetu.push.model.Platform;
import com.cargosetu.push.model.PrincipalType;
import com.cargosetu.push.model.UserPushToken;
import com.cargosetu.push.repository.UserPushTokenRepository;
import com.cargosetu.push.service.PushTokenServiceImpl;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers plan §13: "same device registers twice" is idempotent, token
 * ownership is derived from the principal (never trusted from the client),
 * and Staff vs OTP Customer logins (which share LoggedInUser.staffId) never
 * collide because of the (principalType, principalId) key.
 */
@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private UserPushTokenRepository tokenRepository;

    private PushTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PushTokenServiceImpl(tokenRepository);
    }

    private LoggedInUser staffUser(long id) {
        LoggedInUser user = new LoggedInUser();
        user.setStaffId(id);
        user.setEmail("staff" + id + "@example.com");
        return user;
    }

    private LoggedInUser customerUser(long id) {
        LoggedInUser user = new LoggedInUser();
        user.setStaffId(id); // OTP login reuses this field for the Customer's own id
        user.setRoles(Set.of("CUSTOMER"));
        return user;
    }

    private PushTokenRegisterDTO request(String deviceId, String fcmToken) {
        PushTokenRegisterDTO dto = new PushTokenRegisterDTO();
        dto.setDeviceId(deviceId);
        dto.setFcmToken(fcmToken);
        dto.setPlatform(Platform.WEB);
        dto.setDeviceName("Chrome on Windows");
        return dto;
    }

    @Test
    void sameDeviceRegisteringTwiceUpsertsOneRowNotTwo() {
        LoggedInUser user = staffUser(12);
        when(tokenRepository.findByPrincipalTypeAndPrincipalIdAndDeviceId(PrincipalType.STAFF, 12L, "install-a"))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByFcmToken(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> {
            UserPushToken t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        GenericDataDTO first = service.register(user, request("install-a", "token-1"));
        assertThat(first.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);

        // Second call for the same device finds the existing row instead of creating a new one.
        UserPushToken existing = new UserPushToken();
        existing.setId(1L);
        existing.setPrincipalType(PrincipalType.STAFF);
        existing.setPrincipalId(12L);
        existing.setDeviceId("install-a");
        when(tokenRepository.findByPrincipalTypeAndPrincipalIdAndDeviceId(PrincipalType.STAFF, 12L, "install-a"))
                .thenReturn(Optional.of(existing));

        service.register(user, request("install-a", "token-1-refreshed"));

        ArgumentCaptor<UserPushToken> captor = ArgumentCaptor.forClass(UserPushToken.class);
        verify(tokenRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(t -> t.getId() == null || t.getId().equals(1L));
    }

    @Test
    void staffAndCustomerSharingTheSameNumericIdNeverCollide() {
        when(tokenRepository.findByPrincipalTypeAndPrincipalIdAndDeviceId(any(), anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByFcmToken(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.register(staffUser(105), request("install-a", "staff-token"));
        service.register(customerUser(105), request("install-b", "customer-token"));

        ArgumentCaptor<UserPushToken> captor = ArgumentCaptor.forClass(UserPushToken.class);
        verify(tokenRepository, times(2)).save(captor.capture());
        List<UserPushToken> saved = captor.getAllValues();
        assertThat(saved.get(0).getPrincipalType()).isEqualTo(PrincipalType.STAFF);
        assertThat(saved.get(1).getPrincipalType()).isEqualTo(PrincipalType.CUSTOMER);
        assertThat(saved).extracting(UserPushToken::getPrincipalId).containsOnly(105L);
    }

    @Test
    void deactivateByIdRequiresOwnershipMatch() {
        LoggedInUser user = staffUser(12);
        when(tokenRepository.findByIdAndPrincipalTypeAndPrincipalId(99L, PrincipalType.STAFF, 12L))
                .thenReturn(Optional.empty());

        GenericDataDTO response = service.deactivateById(user, 99L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.NOT_FOUND);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void deactivateByFcmTokenMarksTokenInactiveNotDeleted() {
        UserPushToken token = new UserPushToken();
        token.setId(5L);
        token.setIsActive(true);
        when(tokenRepository.findByFcmToken("bad-token")).thenReturn(Optional.of(token));

        service.deactivateByFcmToken("bad-token");

        assertThat(token.getIsActive()).isFalse();
        verify(tokenRepository).save(token);
        verify(tokenRepository, never()).delete(any());
        verify(tokenRepository, never()).deleteById(any());
    }
}
