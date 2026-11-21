package com.cargosetu.push.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.push.dto.PushTokenRegisterDTO;
import com.cargosetu.utils.GenericDataDTO;

public interface PushTokenService {

    GenericDataDTO register(LoggedInUser user, PushTokenRegisterDTO request);

    GenericDataDTO listMine(LoggedInUser user);

    /** Logout-time unregister of the caller's own current installation. */
    GenericDataDTO deactivateCurrent(LoggedInUser user, String deviceId);

    /** Ownership-checked removal of one of the caller's own device rows. */
    GenericDataDTO deactivateById(LoggedInUser user, Long tokenRecordId);

    /** Called by the delivery consumer when FCM reports a token as unregistered. Not user-facing. */
    void deactivateByFcmToken(String fcmToken);
}
