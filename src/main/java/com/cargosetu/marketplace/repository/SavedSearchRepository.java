package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    List<SavedSearch> findByStaffIdAndActive(Long staffId, Boolean active);
    List<SavedSearch> findByCompanyIdAndActive(Integer companyId, Boolean active);
    List<SavedSearch> findBySearchTypeAndOriginCityAndDestinationCityAndActive(String searchType, String origin, String destination, Boolean active);
}
