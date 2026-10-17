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

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageMapper messageMapper;
    private final MarketplaceAuditService auditService;
    private final com.cargosetu.repo.CompanyRepository companyRepository;
    private final com.cargosetu.repo.StaffRepository staffRepository;
    private final com.cargosetu.marketplace.repository.MarketplaceListingRepository listingRepository;
    private final com.cargosetu.marketplace.repository.MarketplaceBookingRepository bookingRepository;

    @Transactional
    public GenericDataDTO startConversation(LoggedInUser user, StartConversationRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null || user.getStaffId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }

            if (request == null || request.getParticipantCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Participant company is required");
                return response;
            }

            Integer myCompanyId = user.getCompanyId();
            Integer otherCompanyId = request.getParticipantCompanyId();
            Long listingId = request.getListingId();
            Long bookingId = request.getBookingId();

            // Prevent self-conversations
            if (myCompanyId.equals(otherCompanyId)) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot start a conversation with your own company");
                return response;
            }
            if (request.getParticipantStaffId() != null && user.getStaffId().equals(request.getParticipantStaffId())) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot start a conversation with yourself");
                return response;
            }

            // Validate participant company exists
            if (companyRepository.findById(otherCompanyId).isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Participant company not found");
                return response;
            }

            // Validate participant staff belongs to participant company if provided (> 0)
            Long targetStaffId = (request.getParticipantStaffId() != null && request.getParticipantStaffId() > 0)
                    ? request.getParticipantStaffId()
                    : null;
            if (targetStaffId != null) {
                com.cargosetu.model.Staff participantStaff = staffRepository.findById(targetStaffId).orElse(null);
                if (participantStaff == null) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Participant staff not found");
                    return response;
                }
                if (participantStaff.getCompanyId() == null || !participantStaff.getCompanyId().equals(otherCompanyId)) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Participant staff does not belong to the specified participant company");
                    return response;
                }
            }

            // Validate listing relationship and prevent unauthorized participant injection
            if (listingId != null) {
                com.cargosetu.marketplace.entity.MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
                if (listing == null) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Listing not found");
                    return response;
                }
                Integer listingOwnerId = listing.getCompanyId();
                if (listingOwnerId == null || (!listingOwnerId.equals(myCompanyId) && !listingOwnerId.equals(otherCompanyId))) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Unauthorized participant for this listing: conversation must involve the listing owner");
                    return response;
                }
                if (targetStaffId == null && listingOwnerId.equals(otherCompanyId)) {
                    targetStaffId = listing.getPostedByStaffId();
                }
            }

            // Validate booking relationship and participants
            if (bookingId != null) {
                com.cargosetu.marketplace.entity.MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking == null) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Booking not found");
                    return response;
                }
                if (listingId != null && (booking.getListing() == null || !booking.getListing().getId().equals(listingId))) {
                    response.setResponseCode(CommonStatusCode.FAIL);
                    response.setResponseMessege("Booking does not belong to the specified listing");
                    return response;
                }
                if (listingId == null && booking.getListing() != null) {
                    listingId = booking.getListing().getId();
                }
                boolean validBookingParties =
                        (myCompanyId.equals(booking.getShipperCompanyId()) && otherCompanyId.equals(booking.getTransporterCompanyId()))
                        || (myCompanyId.equals(booking.getTransporterCompanyId()) && otherCompanyId.equals(booking.getShipperCompanyId()));
                if (!validBookingParties) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Unauthorized: only booking participants can start a conversation for this booking");
                    return response;
                }
                if (targetStaffId == null) {
                    if (booking.getRequestedByCompanyId() != null && booking.getRequestedByCompanyId().equals(otherCompanyId)) {
                        targetStaffId = booking.getRequestedByStaffId();
                    } else if (booking.getListing() != null && otherCompanyId.equals(booking.getListing().getCompanyId())) {
                        targetStaffId = booking.getListing().getPostedByStaffId();
                    }
                }
            }

            // Look for existing conversation and validate access when reusing
            ChatConversation convo = null;
            if (bookingId != null) {
                java.util.List<ChatConversation> bookingConvos = conversationRepository.findByBookingId(bookingId);
                if (bookingConvos != null && !bookingConvos.isEmpty()) {
                    for (ChatConversation existing : bookingConvos) {
                        if (!isParticipant(myCompanyId, existing) || !isParticipant(otherCompanyId, existing)) {
                            response.setResponseCode(CommonStatusCode.FORBIDDEN);
                            response.setResponseMessege("Unauthorized to access existing booking conversation");
                            return response;
                        }
                        convo = existing;
                        break;
                    }
                }
            }
            if (convo == null && listingId != null) {
                convo = conversationRepository.findByParticipant1CompanyIdAndParticipant2CompanyIdAndListingId(myCompanyId, otherCompanyId, listingId)
                        .orElse(null);
                if (convo == null) {
                    convo = conversationRepository.findByParticipant1CompanyIdAndParticipant2CompanyIdAndListingId(otherCompanyId, myCompanyId, listingId)
                            .orElse(null);
                }
                if (convo != null && (!isParticipant(myCompanyId, convo) || !isParticipant(otherCompanyId, convo))) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Unauthorized to access existing conversation");
                    return response;
                }
            }

            if (convo == null) {
                convo = new ChatConversation();
                convo.setListingId(listingId);
                convo.setBookingId(bookingId);
                convo.setParticipant1CompanyId(myCompanyId);
                convo.setParticipant1StaffId(user.getStaffId());
                convo.setParticipant2CompanyId(otherCompanyId);
                convo.setParticipant2StaffId(targetStaffId);
                convo.setStatus("ACTIVE");
                convo.setLastMessageAt(LocalDateTime.now());
                convo = conversationRepository.save(convo);
            }

            if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
                ChatMessage msg = new ChatMessage();
                msg.setConversationId(convo.getId());
                msg.setSenderStaffId(user.getStaffId());
                msg.setSenderCompanyId(myCompanyId);
                msg.setMessageType("TEXT");
                msg.setMessage(request.getInitialMessage().trim());
                msg.setSentAt(LocalDateTime.now());
                messageRepository.save(msg);
                convo.setLastMessageAt(LocalDateTime.now());
                conversationRepository.save(convo);
            }

            auditService.logEvent(myCompanyId, user.getStaffId(), "ChatConversation", convo.getId(), "START", null, "ACTIVE");

            response.setData(toConversationResponse(convo, user));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Conversation started successfully");
        } catch (Exception e) {
            log.error("Error starting conversation", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to start conversation: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO sendMessage(LoggedInUser user, Long conversationId, ChatMessageRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Message text cannot be empty");
                return response;
            }

            ChatConversation convo = conversationRepository.findById(conversationId).orElse(null);
            if (convo == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Conversation not found");
                return response;
            }

            // Authorization check: user must belong to one of the participant companies
            if (!isParticipant(user != null ? user.getCompanyId() : null, convo)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized to send message in this conversation");
                return response;
            }

            ChatMessage msg = new ChatMessage();
            msg.setConversationId(convo.getId());
            msg.setSenderStaffId(user.getStaffId());
            msg.setSenderCompanyId(user.getCompanyId());
            msg.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
            msg.setMessage(request.getMessage().trim());
            msg.setAttachmentRef(request.getAttachmentRef());
            msg.setSentAt(LocalDateTime.now());
            msg = messageRepository.save(msg);

            convo.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(convo);

            com.cargosetu.marketplace.dto.ChatMessageResponse respDto = toChatMessageResponse(msg);
            response.setData(respDto);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Message sent successfully");
        } catch (Exception e) {
            log.error("Error sending message", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to send message: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getConversations(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            java.util.List<ChatConversation> convos = conversationRepository.findByParticipantCompanyId(user.getCompanyId());
            java.util.List<com.cargosetu.marketplace.dto.ConversationResponse> dtos = convos.stream()
                    .map(c -> toConversationResponse(c, user))
                    .sorted((a, b) -> {
                        if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) return 0;
                        if (a.getLastMessageAt() == null) return 1;
                        if (b.getLastMessageAt() == null) return -1;
                        return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                    })
                    .toList();
            response.setDataList(dtos);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Conversations retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting conversations", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get conversations: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getMessages(LoggedInUser user, Long conversationId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            ChatConversation convo = conversationRepository.findById(conversationId).orElse(null);
            if (convo == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Conversation not found");
                return response;
            }
            if (!isParticipant(user != null ? user.getCompanyId() : null, convo)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized to view this conversation");
                return response;
            }

            java.util.List<ChatMessage> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
            if (messages == null) {
                messages = java.util.Collections.emptyList();
            }

            // Batch-load sender staff names and company names to avoid N+1 queries
            java.util.Set<Long> staffIds = messages.stream()
                    .map(ChatMessage::getSenderStaffId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            java.util.Set<Integer> companyIds = messages.stream()
                    .map(ChatMessage::getSenderCompanyId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.Map<Long, String> staffNameMap = new java.util.HashMap<>();
            if (!staffIds.isEmpty()) {
                java.util.List<com.cargosetu.model.Staff> staffs = staffRepository.findAllById(staffIds);
                if (staffs != null) {
                    for (com.cargosetu.model.Staff s : staffs) {
                        if (s != null && s.getStaffId() != null) {
                            staffNameMap.put(s.getStaffId(), formatStaffName(s));
                        }
                    }
                }
                for (Long sid : staffIds) {
                    if (!staffNameMap.containsKey(sid)) {
                        staffRepository.findById(sid).ifPresent(s -> staffNameMap.put(sid, formatStaffName(s)));
                    }
                }
            }

            java.util.Map<Integer, String> companyNameMap = new java.util.HashMap<>();
            if (!companyIds.isEmpty()) {
                java.util.List<com.cargosetu.model.Company> companies = companyRepository.findAllById(companyIds);
                if (companies != null) {
                    for (com.cargosetu.model.Company comp : companies) {
                        if (comp != null && comp.getCompanyId() != null) {
                            companyNameMap.put(comp.getCompanyId(), comp.getCompanyName());
                        }
                    }
                }
                for (Integer cid : companyIds) {
                    if (!companyNameMap.containsKey(cid)) {
                        companyRepository.findById(cid).ifPresent(comp -> companyNameMap.put(cid, comp.getCompanyName()));
                    }
                }
            }

            java.util.List<com.cargosetu.marketplace.dto.ChatMessageResponse> dtos = messages.stream()
                    .map(m -> toChatMessageResponse(m, staffNameMap, companyNameMap))
                    .toList();
            response.setDataList(dtos);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Messages retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting messages", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get messages: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO markAsRead(LoggedInUser user, Long conversationId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }

            ChatConversation convo = conversationRepository.findById(conversationId).orElse(null);
            if (convo == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Conversation not found");
                return response;
            }

            if (!isParticipant(user.getCompanyId(), convo)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized to mark messages as read in this conversation");
                return response;
            }

            java.util.List<ChatMessage> unreadMessages = messageRepository.findByConversationIdAndReadAtIsNullAndSenderStaffIdNot(conversationId, user.getStaffId());
            for (ChatMessage m : unreadMessages) {
                m.setReadAt(LocalDateTime.now());
            }
            messageRepository.saveAll(unreadMessages);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Messages marked as read");
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to mark messages as read: " + e.getMessage());
        }
        return response;
    }

    private boolean isParticipant(Integer companyId, ChatConversation convo) {
        if (companyId == null || convo == null) return false;
        return companyId.equals(convo.getParticipant1CompanyId()) || companyId.equals(convo.getParticipant2CompanyId());
    }

    private com.cargosetu.marketplace.dto.ConversationResponse toConversationResponse(ChatConversation c, LoggedInUser user) {
        if (c == null) return null;
        com.cargosetu.marketplace.dto.ConversationResponse dto = com.cargosetu.marketplace.dto.ConversationResponse.builder()
                .id(c.getId())
                .listingId(c.getListingId())
                .bookingId(c.getBookingId())
                .participant1CompanyId(c.getParticipant1CompanyId())
                .participant1StaffId(c.getParticipant1StaffId())
                .participant2CompanyId(c.getParticipant2CompanyId())
                .participant2StaffId(c.getParticipant2StaffId())
                .status(c.getStatus())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .build();

        if (c.getParticipant1CompanyId() != null) {
            companyRepository.findById(c.getParticipant1CompanyId()).ifPresent(comp -> dto.setParticipant1Name(comp.getCompanyName()));
        }
        if (c.getParticipant2CompanyId() != null) {
            companyRepository.findById(c.getParticipant2CompanyId()).ifPresent(comp -> dto.setParticipant2Name(comp.getCompanyName()));
        }

        long unread = messageRepository.countByConversationIdAndReadAtIsNullAndSenderStaffIdNot(c.getId(), user.getStaffId());
        dto.setUnreadCount(unread);

        // Fetch last message preview
        java.util.List<ChatMessage> msgs = messageRepository.findByConversationIdOrderBySentAtAsc(c.getId());
        if (msgs != null && !msgs.isEmpty()) {
            ChatMessage last = msgs.get(msgs.size() - 1);
            dto.setLastMessage(last.getMessage());
            if (dto.getLastMessageAt() == null) {
                dto.setLastMessageAt(last.getSentAt());
            }
        }
        return dto;
    }

    private String formatStaffName(com.cargosetu.model.Staff s) {
        if (s == null) return null;
        if (s.getFirstName() != null && !s.getFirstName().isBlank()) {
            return (s.getFirstName().trim() + (s.getLastName() != null && !s.getLastName().isBlank() ? " " + s.getLastName().trim() : "")).trim();
        }
        return s.getUserName();
    }

    private com.cargosetu.marketplace.dto.ChatMessageResponse toChatMessageResponse(ChatMessage msg) {
        return toChatMessageResponse(msg, null, null);
    }

    private com.cargosetu.marketplace.dto.ChatMessageResponse toChatMessageResponse(
            ChatMessage msg,
            java.util.Map<Long, String> staffNameMap,
            java.util.Map<Integer, String> companyNameMap) {
        if (msg == null) return null;
        com.cargosetu.marketplace.dto.ChatMessageResponse mapped = messageMapper.toResponse(msg);
        com.cargosetu.marketplace.dto.ChatMessageResponse dto = mapped != null
                ? mapped
                : com.cargosetu.marketplace.dto.ChatMessageResponse.builder().build();
        dto.setId(msg.getId());
        dto.setConversationId(msg.getConversationId());
        dto.setSenderStaffId(msg.getSenderStaffId());
        dto.setSenderCompanyId(msg.getSenderCompanyId());
        dto.setMessageType(msg.getMessageType());
        dto.setMessage(msg.getMessage());
        dto.setAttachmentRef(msg.getAttachmentRef());
        dto.setSentAt(msg.getSentAt());
        dto.setDeliveredAt(msg.getDeliveredAt());
        dto.setReadAt(msg.getReadAt());

        if (msg.getSenderStaffId() != null) {
            String staffName = staffNameMap != null ? staffNameMap.get(msg.getSenderStaffId()) : null;
            if (staffName == null) {
                com.cargosetu.model.Staff s = staffRepository.findById(msg.getSenderStaffId()).orElse(null);
                staffName = formatStaffName(s);
            }
            dto.setSenderName(staffName);
            dto.setSenderStaffName(staffName);
        }
        if (msg.getSenderCompanyId() != null) {
            String companyName = companyNameMap != null ? companyNameMap.get(msg.getSenderCompanyId()) : null;
            if (companyName == null) {
                com.cargosetu.model.Company comp = companyRepository.findById(msg.getSenderCompanyId()).orElse(null);
                if (comp != null) {
                    companyName = comp.getCompanyName();
                }
            }
            dto.setSenderCompanyName(companyName);
        }
        return dto;
    }
}
