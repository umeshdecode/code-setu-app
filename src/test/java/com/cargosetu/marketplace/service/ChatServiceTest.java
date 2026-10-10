package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.ChatMessageRequest;
import com.cargosetu.marketplace.dto.StartConversationRequest;
import com.cargosetu.marketplace.entity.ChatConversation;
import com.cargosetu.marketplace.entity.ChatMessage;
import com.cargosetu.marketplace.mapper.ChatMessageMapper;
import com.cargosetu.marketplace.repository.ChatConversationRepository;
import com.cargosetu.marketplace.repository.ChatMessageRepository;
import com.cargosetu.model.Company;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatConversationRepository conversationRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ChatMessageMapper messageMapper;
    @Mock private MarketplaceAuditService auditService;
    @Mock private CompanyRepository companyRepository;
    @Mock private StaffRepository staffRepository;

    @InjectMocks
    private ChatService chatService;

    private LoggedInUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new LoggedInUser();
        currentUser.setCompanyId(100);
        currentUser.setStaffId(5L);
    }

    @Test
    @DisplayName("startConversation creates conversation and saves initial message if provided")
    void startConversation_createsNew() {
        StartConversationRequest request = StartConversationRequest.builder()
                .listingId(12L)
                .participantCompanyId(200)
                .participantStaffId(8L)
                .initialMessage("Hello, is the load still available?")
                .build();

        when(conversationRepository.findByParticipant1CompanyIdAndParticipant2CompanyIdAndListingId(100, 200, 12L))
                .thenReturn(Optional.empty());
        when(conversationRepository.findByParticipant1CompanyIdAndParticipant2CompanyIdAndListingId(200, 100, 12L))
                .thenReturn(Optional.empty());

        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(i -> {
            ChatConversation c = i.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(messageRepository.findByConversationIdOrderBySentAtAsc(any())).thenReturn(Collections.emptyList());

        Company senderComp = new Company();
        senderComp.setCompanyName("Sender Corp");
        when(companyRepository.findById(100)).thenReturn(Optional.of(senderComp));

        Company otherComp = new Company();
        otherComp.setCompanyName("Receiver Corp");
        when(companyRepository.findById(200)).thenReturn(Optional.of(otherComp));

        GenericDataDTO response = chatService.startConversation(currentUser, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(response.getResponseMessege()).isEqualTo("Conversation started successfully");
        verify(conversationRepository, atLeastOnce()).save(any(ChatConversation.class));
        verify(messageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage succeeds for active participant")
    void sendMessage_success() {
        ChatConversation convo = new ChatConversation();
        convo.setId(10L);
        convo.setParticipant1CompanyId(100);
        convo.setParticipant2CompanyId(200);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("We can provide 2 trailers tomorrow morning.")
                .build();

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(convo));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(i -> {
            ChatMessage m = i.getArgument(0);
            m.setId(101L);
            return m;
        });

        GenericDataDTO response = chatService.sendMessage(currentUser, 10L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(response.getResponseMessege()).isEqualTo("Message sent successfully");
        verify(messageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage fails when text is empty")
    void sendMessage_failsWhenEmpty() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("   ")
                .build();

        GenericDataDTO response = chatService.sendMessage(currentUser, 10L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("cannot be empty");
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("markAsRead marks unread messages from other party")
    void markAsRead_success() {
        ChatMessage unread = new ChatMessage();
        unread.setId(1L);
        unread.setConversationId(10L);
        unread.setSenderStaffId(8L);

        when(messageRepository.findByConversationIdAndReadAtIsNullAndSenderStaffIdNot(10L, 5L))
                .thenReturn(Collections.singletonList(unread));

        GenericDataDTO response = chatService.markAsRead(currentUser, 10L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(response.getResponseMessege()).contains("read");
        assertThat(unread.getReadAt()).isNotNull();
        verify(messageRepository).saveAll(Collections.singletonList(unread));
    }
}
