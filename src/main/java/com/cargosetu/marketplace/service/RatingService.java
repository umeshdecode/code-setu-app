package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.RatingRequest;
import com.cargosetu.marketplace.dto.RatingResponse;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.RatingReview;
import com.cargosetu.marketplace.enums.BookingStatus;
import com.cargosetu.marketplace.mapper.RatingReviewMapper;
import com.cargosetu.marketplace.repository.MarketplaceBookingRepository;
import com.cargosetu.marketplace.repository.RatingReviewRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {

    private final RatingReviewRepository ratingReviewRepository;
    private final MarketplaceBookingRepository bookingRepository;
    private final RatingReviewMapper ratingReviewMapper;
    private final MarketplaceAuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenericDataDTO submitRating(LoggedInUser user, Long bookingId, RatingRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<MarketplaceBooking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found with ID: " + bookingId);
                return response;
            }

            MarketplaceBooking booking = bookingOpt.get();

            // Validate booking is completed
            if (!BookingStatus.COMPLETED.name().equals(booking.getStatus())) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Ratings can only be submitted for completed bookings");
                return response;
            }

            // Validate one rating per company per booking
            if (ratingReviewRepository.existsByBookingIdAndReviewerCompanyId(bookingId, user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("You have already submitted a rating for this booking");
                return response;
            }

            // Determine reviewee company
            Integer revieweeCompanyId;
            if (user.getCompanyId().equals(booking.getShipperCompanyId())) {
                revieweeCompanyId = booking.getTransporterCompanyId();
            } else {
                revieweeCompanyId = booking.getShipperCompanyId();
            }

            RatingReview rating = ratingReviewMapper.toEntity(request);
            rating.setBookingId(bookingId);
            rating.setReviewerCompanyId(user.getCompanyId());
            rating.setReviewerStaffId(user.getStaffId());
            rating.setRevieweeCompanyId(revieweeCompanyId);

            // Convert tags list to JSON
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                try {
                    rating.setTags(objectMapper.writeValueAsString(request.getTags()));
                } catch (Exception e) {
                    log.warn("Failed to serialize tags: {}", e.getMessage());
                }
            }

            ratingReviewRepository.save(rating);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(),
                    "RATING_REVIEW", rating.getId(), "CREATED", null, null);

            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Rating submitted successfully!");
            response.setData(ratingReviewMapper.toResponse(rating));

        } catch (Exception e) {
            log.error("Error submitting rating for booking {}: {}", bookingId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while submitting rating.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getRatingsForBooking(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<RatingReview> ratings = ratingReviewRepository.findByBookingId(bookingId);
            List<RatingResponse> dtos = ratings.stream()
                    .map(ratingReviewMapper::toResponse)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Ratings fetched successfully!");
            response.setData(dtos);
        } catch (Exception e) {
            log.error("Error fetching ratings for booking {}: {}", bookingId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching ratings.");
        }
        return response;
    }

    public GenericDataDTO getCompanyRatings(Integer companyId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<RatingReview> ratings = ratingReviewRepository.findByRevieweeCompanyId(companyId);
            List<RatingResponse> dtos = ratings.stream()
                    .map(ratingReviewMapper::toResponse)
                    .toList();

            Double averageRating = ratingReviewRepository.getAverageRatingByRevieweeCompanyId(companyId);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Company ratings fetched successfully!");
            response.setData(java.util.Map.of(
                    "ratings", dtos,
                    "averageRating", averageRating != null ? averageRating : 0.0,
                    "totalReviews", dtos.size()
            ));
        } catch (Exception e) {
            log.error("Error fetching ratings for company {}: {}", companyId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching company ratings.");
        }
        return response;
    }
}
