package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.ListingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingDocumentRepository extends JpaRepository<ListingDocument, Long> {
    List<ListingDocument> findByListingId(Long listingId);
}
