package com.cargosetu.marketplace;

import com.cargosetu.marketplace.dto.CounterBookingRequest;
import com.cargosetu.marketplace.dto.CreateLoadPostRequest;
import com.cargosetu.marketplace.dto.CreateTruckPostRequest;
import com.cargosetu.marketplace.enums.CapacityUnit;
import com.cargosetu.marketplace.enums.CargoType;
import com.cargosetu.marketplace.enums.TruckAvailabilityStatus;
import com.cargosetu.marketplace.util.MarketplaceNumbers;
import com.cargosetu.marketplace.util.MarketplaceResponses;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Request validation rules and the small marketplace utilities. */
class MarketplaceValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CreateLoadPostRequest validLoad() {
        CreateLoadPostRequest r = new CreateLoadPostRequest();
        r.setOriginCity("Ahmedabad");
        r.setDestinationCity("Mumbai");
        r.setPickupFrom(LocalDateTime.now().plusDays(1));
        r.setCargoType(CargoType.values()[0]);
        r.setWeight(12.0);
        r.setWeightUnit(CapacityUnit.values()[0]);
        return r;
    }

    @Test
    void validLoadPasses() {
        assertTrue(validator.validate(validLoad()).isEmpty());
    }

    @Test
    void loadRejectsZeroWeightSameCityPastPickupAndBadAdvance() {
        CreateLoadPostRequest r = validLoad();
        r.setWeight(0.0);
        assertFalse(validator.validate(r).isEmpty());

        r = validLoad();
        r.setDestinationCity(" ahmedabad ");
        assertFalse(validator.validate(r).isEmpty());

        r = validLoad();
        r.setPickupFrom(LocalDateTime.now().minusDays(2));
        assertFalse(validator.validate(r).isEmpty());

        r = validLoad();
        r.setDeliveryTo(r.getPickupFrom().minusDays(1));
        assertFalse(validator.validate(r).isEmpty());

        r = validLoad();
        r.setAdvancePercentage(150);
        assertFalse(validator.validate(r).isEmpty());
    }

    @Test
    void truckRejectsAvailableAboveTotalCapacity() {
        CreateTruckPostRequest r = new CreateTruckPostRequest();
        r.setVehicleId(1L);
        r.setCurrentCity("Pune");
        r.setTotalCapacity(10.0);
        r.setAvailableCapacity(12.0);
        r.setCapacityUnit(CapacityUnit.values()[0]);
        r.setTruckStatus(TruckAvailabilityStatus.values()[0]);
        assertFalse(validator.validate(r).isEmpty());

        r.setAvailableCapacity(8.0);
        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void counterBookingAcceptsLegacyCounterFreightField() throws Exception {
        CounterBookingRequest r = new ObjectMapper().readValue("{\"counterFreight\": 18000}", CounterBookingRequest.class);
        assertEquals(0, new BigDecimal("18000").compareTo(r.getProposedFreight()));
    }

    @Test
    void referenceNumbersAreUniqueAndPrefixed() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String n = MarketplaceNumbers.next("LD");
            assertTrue(n.matches("LD-\\d{8}-[0-9A-Z]{6}"), n);
            seen.add(n);
        }
        assertEquals(2000, seen.size());
    }

    @Test
    void businessForbiddenIsNotSentAsHttp403() {
        GenericDataDTO body = MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN, "Cannot bid on own listing");
        assertEquals(409, MarketplaceResponses.of(body).getStatusCode().value());
        assertEquals(CommonStatusCode.FORBIDDEN, body.getResponseCode());

        GenericDataDTO notFound = MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "x");
        assertEquals(404, MarketplaceResponses.of(notFound).getStatusCode().value());
    }
}
