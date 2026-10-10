package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.RatingReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingReviewRepository extends JpaRepository<RatingReview, Long> {
    List<RatingReview> findByBookingId(Long bookingId);
    List<RatingReview> findByRevieweeCompanyId(Integer companyId);
    boolean existsByBookingIdAndReviewerCompanyId(Long bookingId, Integer companyId);
    
    @Query("SELECT AVG(r.overallRating) FROM RatingReview r WHERE r.revieweeCompanyId = :companyId")
    Double getAverageRatingByRevieweeCompanyId(@Param("companyId") Integer companyId);
}
