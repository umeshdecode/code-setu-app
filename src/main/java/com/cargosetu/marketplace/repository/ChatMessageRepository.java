package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    List<ChatMessage> findByConversationIdAndReadAtIsNullAndSenderStaffIdNot(Long conversationId, Long staffId);
    long countByConversationIdAndReadAtIsNullAndSenderStaffIdNot(Long conversationId, Long staffId);
}
