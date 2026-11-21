package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByListingId(Long listingId);
    List<ChatConversation> findByBookingId(Long bookingId);
    
    @Query("SELECT c FROM ChatConversation c WHERE c.participant1CompanyId = :companyId OR c.participant2CompanyId = :companyId")
    List<ChatConversation> findByParticipantCompanyId(@Param("companyId") Integer companyId);
    
    Optional<ChatConversation> findByParticipant1CompanyIdAndParticipant2CompanyIdAndListingId(Integer c1, Integer c2, Long listingId);
}
