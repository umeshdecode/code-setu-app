package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.ChatService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.marketplace.util.MarketplaceResponses;
import com.cargosetu.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.CONVERSATIONS)
public class MarketplaceChatController {

    private final ChatService chatService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_CHAT_CREATE + "')")
    public ResponseEntity<?> startConversation(@AuthenticationPrincipal LoggedInUser user, @Valid @RequestBody StartConversationRequest request) {
        log.info("API entry for startConversation by user: {}", user.getStaffId());
        try {
            GenericDataDTO response = chatService.startConversation(user, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in startConversation", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_CHAT_READ + "')")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getConversations user: {}", user.getStaffId());
        try {
            GenericDataDTO response = chatService.getConversations(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getConversations", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/{id}" + MarketplaceUrlConstants.MESSAGES)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_CHAT_READ + "')")
    public ResponseEntity<?> getMessages(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for getMessages id: {}", id);
        try {
            GenericDataDTO response = chatService.getMessages(user, id);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getMessages", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}" + MarketplaceUrlConstants.MESSAGES)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_CHAT_CREATE + "')")
    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody ChatMessageRequest request) {
        log.info("API entry for sendMessage id: {}", id);
        try {
            GenericDataDTO response = chatService.sendMessage(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in sendMessage", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_CHAT_UPDATE + "')")
    public ResponseEntity<?> markAsRead(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for markAsRead id: {}", id);
        try {
            GenericDataDTO response = chatService.markAsRead(user, id);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in markAsRead", e);
            return MarketplaceResponses.error();
        }
    }
}
