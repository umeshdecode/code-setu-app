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
    @Mock private com.cargosetu.marketplace.repository.MarketplaceListingRepository listingRepository;
    @Mock private com.cargosetu.marketplace.repository.MarketplaceBookingRepository bookingRepository;

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

        com.cargosetu.model.Staff otherStaff = new com.cargosetu.model.Staff();
        otherStaff.setStaffId(8L);
        otherStaff.setCompanyId(200);
        when(staffRepository.findById(8L)).thenReturn(Optional.of(otherStaff));

        com.cargosetu.marketplace.entity.MarketplaceListing listing = new com.cargosetu.marketplace.entity.MarketplaceListing();
        listing.setId(12L);
        listing.setCompanyId(200);
        when(listingRepository.findById(12L)).thenReturn(Optional.of(listing));

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
        ChatConversation convo = new ChatConversation();
        convo.setId(10L);
        convo.setParticipant1CompanyId(100);
        convo.setParticipant2CompanyId(200);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(convo));

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

    @Test
    @DisplayName("markAsRead fails with NOT_FOUND when conversation does not exist")
    void markAsRead_failsWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        GenericDataDTO response = chatService.markAsRead(currentUser, 999L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.NOT_FOUND);
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("markAsRead fails with FORBIDDEN when user company is not a participant")
    void markAsRead_failsWhenUnauthorizedParticipant() {
        ChatConversation convo = new ChatConversation();
        convo.setId(10L);
        convo.setParticipant1CompanyId(200);
        convo.setParticipant2CompanyId(300);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(convo));

        GenericDataDTO response = chatService.markAsRead(currentUser, 10L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("startConversation fails when attempting self-conversation")
    void startConversation_failsWhenSelfConversation() {
        StartConversationRequest request = StartConversationRequest.builder()
                .listingId(12L)
                .participantCompanyId(100) // same as currentUser
                .build();

        GenericDataDTO response = chatService.startConversation(currentUser, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("own company");
        verifyNoInteractions(conversationRepository);
    }

    @Test
    @DisplayName("startConversation fails with FORBIDDEN when staff does not belong to participant company")
    void startConversation_failsWhenStaffBelongsToWrongCompany() {
        StartConversationRequest request = StartConversationRequest.builder()
                .listingId(12L)
                .participantCompanyId(200)
                .participantStaffId(8L)
                .build();

        when(companyRepository.findById(200)).thenReturn(Optional.of(new Company()));

        com.cargosetu.model.Staff mismatchedStaff = new com.cargosetu.model.Staff();
        mismatchedStaff.setStaffId(8L);
        mismatchedStaff.setCompanyId(999); // does not belong to company 200
        when(staffRepository.findById(8L)).thenReturn(Optional.of(mismatchedStaff));

        GenericDataDTO response = chatService.startConversation(currentUser, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    @DisplayName("startConversation fails with FORBIDDEN on unauthorized participant injection for listing")
    void startConversation_failsWhenNeitherParticipantOwnsListing() {
        StartConversationRequest request = StartConversationRequest.builder()
                .listingId(12L)
                .participantCompanyId(200) // neither 100 nor 200 owns listing 12
                .build();

        when(companyRepository.findById(200)).thenReturn(Optional.of(new Company()));

        com.cargosetu.marketplace.entity.MarketplaceListing listing = new com.cargosetu.marketplace.entity.MarketplaceListing();
        listing.setId(12L);
        listing.setCompanyId(300); // owned by company 300
        when(listingRepository.findById(12L)).thenReturn(Optional.of(listing));

        GenericDataDTO response = chatService.startConversation(currentUser, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    @DisplayName("startConversation fails with FORBIDDEN when participants do not match booking parties")
    void startConversation_failsWhenUnauthorizedBookingParticipants() {
        StartConversationRequest request = StartConversationRequest.builder()
                .bookingId(50L)
                .participantCompanyId(200)
                .build();

        when(companyRepository.findById(200)).thenReturn(Optional.of(new Company()));

        com.cargosetu.marketplace.entity.MarketplaceBooking booking = new com.cargosetu.marketplace.entity.MarketplaceBooking();
        booking.setId(50L);
        booking.setShipperCompanyId(100);
        booking.setTransporterCompanyId(300); // company 200 is not part of this booking
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));

        GenericDataDTO response = chatService.startConversation(currentUser, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    @DisplayName("getMessages populates senderStaffId, senderStaffName, senderCompanyId, and senderCompanyName")
    void getMessages_populatesSenderIdentity() {
        ChatConversation convo = new ChatConversation();
        convo.setId(10L);
        convo.setParticipant1CompanyId(100);
        convo.setParticipant2CompanyId(200);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(convo));

        ChatMessage m1 = new ChatMessage();
        m1.setId(1L);
        m1.setConversationId(10L);
        m1.setSenderStaffId(5L);
        m1.setSenderCompanyId(100);
        m1.setMessage("Can you load by 9 AM?");

        ChatMessage m2 = new ChatMessage();
        m2.setId(2L);
        m2.setConversationId(10L);
        m2.setSenderStaffId(8L);
        m2.setSenderCompanyId(200);
        m2.setMessage("Yes, truck is at Bhiwandi naka.");

        when(messageRepository.findByConversationIdOrderBySentAtAsc(10L))
                .thenReturn(java.util.List.of(m1, m2));

        com.cargosetu.model.Staff s1 = new com.cargosetu.model.Staff();
        s1.setStaffId(5L);
        s1.setFirstName("Amit");
        s1.setLastName("Sharma");
        com.cargosetu.model.Staff s2 = new com.cargosetu.model.Staff();
        s2.setStaffId(8L);
        s2.setFirstName("Rajesh");
        s2.setLastName("Patil");
        when(staffRepository.findAllById(any())).thenReturn(java.util.List.of(s1, s2));

        Company c1 = new Company();
        c1.setCompanyId(100);
        c1.setCompanyName("Apex Shippers");
        Company c2 = new Company();
        c2.setCompanyId(200);
        c2.setCompanyName("Patil Roadlines");
        when(companyRepository.findAllById(any())).thenReturn(java.util.List.of(c1, c2));

        GenericDataDTO res = chatService.getMessages(currentUser, 10L);
        assertThat(res.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(res.getDataList()).hasSize(2);

        com.cargosetu.marketplace.dto.ChatMessageResponse r1 =
                (com.cargosetu.marketplace.dto.ChatMessageResponse) res.getDataList().get(0);
        com.cargosetu.marketplace.dto.ChatMessageResponse r2 =
                (com.cargosetu.marketplace.dto.ChatMessageResponse) res.getDataList().get(1);

        assertThat(r1.getSenderStaffId()).isEqualTo(5L);
        assertThat(r1.getSenderStaffName()).isEqualTo("Amit Sharma");
        assertThat(r1.getSenderCompanyName()).isEqualTo("Apex Shippers");

        assertThat(r2.getSenderStaffId()).isEqualTo(8L);
        assertThat(r2.getSenderStaffName()).isEqualTo("Rajesh Patil");
        assertThat(r2.getSenderCompanyName()).isEqualTo("Patil Roadlines");
    }
}
