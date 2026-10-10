package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.ChatMessageRequest;
import com.cargosetu.marketplace.dto.StartConversationRequest;
import com.cargosetu.marketplace.entity.ChatConversation;
import com.cargosetu.marketplace.entity.ChatMessage;
import com.cargosetu.marketplace.mapper.ChatMessageMapper;
import com.cargosetu.marketplace.repository.ChatConversationRepository;
import com.cargosetu.marketplace.repository.ChatMessageRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageMapper messageMapper;
    private final MarketplaceAuditService auditService;
    // Optional SimpMessagingTemplate could be injected here

    @Transactional
    public GenericDataDTO startConversation(LoggedInUser user, StartConversationRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            ChatConversation convo = new ChatConversation();
            convo.setListingId(request.getListingId());
            convo.setBookingId(request.getBookingId());
            convo = conversationRepository.save(convo);

            if (request.getInitialMessage() != null) {
                ChatMessage msg = new ChatMessage();
                msg.setConversationId(convo.getId());
                msg.setSenderStaffId(user.getStaffId());
                msg.setMessage(request.getInitialMessage());
                messageRepository.save(msg);
            }

            response.setResponseCode(CommonStatusCode.CREATED);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO sendMessage(LoggedInUser user, Long conversationId, ChatMessageRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            ChatConversation convo = conversationRepository.findById(conversationId).orElseThrow();
            ChatMessage msg = new ChatMessage();
            msg.setConversationId(convo.getId());
            msg.setSenderStaffId(user.getStaffId());
            msg.setMessage(request.getMessage());
            messageRepository.save(msg);
            
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getConversations(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    public GenericDataDTO getMessages(LoggedInUser user, Long conversationId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO markAsRead(LoggedInUser user, Long conversationId) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }
}
