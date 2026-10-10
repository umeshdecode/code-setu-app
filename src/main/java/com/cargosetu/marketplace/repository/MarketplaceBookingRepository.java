package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceBookingRepository extends JpaRepository<MarketplaceBooking, Long>, JpaSpecificationExecutor<MarketplaceBooking> {
    Optional<MarketplaceBooking> findByBookingNumber(String bookingNumber);
    List<MarketplaceBooking> findByShipperCompanyIdAndStatus(Integer companyId, BookingStatus status);
    List<MarketplaceBooking> findByTransporterCompanyIdAndStatus(Integer companyId, BookingStatus status);
    List<MarketplaceBooking> findByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);
    List<MarketplaceBooking> findByShipperCompanyIdOrTransporterCompanyId(Integer shipperId, Integer transporterId);
    long countByShipperCompanyId(Integer companyId);
    long countByTransporterCompanyId(Integer companyId);
}
