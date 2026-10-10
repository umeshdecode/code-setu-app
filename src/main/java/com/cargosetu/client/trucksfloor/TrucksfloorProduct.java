package com.cargosetu.client.trucksfloor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorProduct {

    private String id;
    private String productId;
    private String brandVehicleName;
    private String slug;
    private String vehicleName;
    private String category;
    private String subCategory;
    private String series;
    private String seriesSlug;
    private String primarySlug;

    private JsonNode brochure;

    private TrucksfloorEngineDto engine;
    private TrucksfloorDimensionDto dimension;
    private TrucksfloorTransmissionLoadingDto transmissionLoading;
    private TrucksfloorBrakeSuspensionDto brakeSuspension;
    private TrucksfloorBodyOptionCabinTypeDto bodyOptionCabinType;
    private TrucksfloorTyresDto tyres;
    private JsonNode others;
    private TrucksfloorPriceDto price;

    private JsonNode images;
    private JsonNode expertReview;

    /**
     * Extracts brochure URL safely whether brochure is a String or an Object.
     */
    public String extractBrochureUrl() {
        if (brochure == null || brochure.isNull()) {
            return null;
        }
        if (brochure.isTextual()) {
            String text = brochure.asText().trim();
            return text.isEmpty() ? null : text;
        }
        if (brochure.isObject()) {
            if (brochure.hasNonNull("url")) {
                return brochure.get("url").asText();
            }
            if (brochure.hasNonNull("file")) {
                return brochure.get("file").asText();
            }
            if (brochure.hasNonNull("path")) {
                return brochure.get("path").asText();
            }
        }
        return null;
    }

    /**
     * Extracts image URLs safely whether images is a list of strings or list of objects.
     */
    public List<String> extractImageUrls() {
        List<String> result = new ArrayList<>();
        if (images == null || images.isNull()) {
            return result;
        }
        if (images.isArray()) {
            for (JsonNode node : images) {
                if (node.isTextual()) {
                    String url = node.asText().trim();
                    if (!url.isEmpty()) {
                        result.add(url);
                    }
                } else if (node.isObject()) {
                    if (node.hasNonNull("url")) {
                        result.add(node.get("url").asText());
                    } else if (node.hasNonNull("imageUrl")) {
                        result.add(node.get("imageUrl").asText());
                    } else if (node.hasNonNull("src")) {
                        result.add(node.get("src").asText());
                    } else if (node.hasNonNull("path")) {
                        result.add(node.get("path").asText());
                    }
                }
            }
        } else if (images.isTextual()) {
            String url = images.asText().trim();
            if (!url.isEmpty()) {
                result.add(url);
            }
        }
        return result;
    }

    public String resolveProductId() {
        if (productId != null && !productId.trim().isEmpty()) {
            return productId.trim();
        }
        if (id != null && !id.trim().isEmpty()) {
            return id.trim();
        }
        if (slug != null && !slug.trim().isEmpty()) {
            return slug.trim();
        }
        return null;
    }

    public String resolveModelName() {
        if (vehicleName != null && !vehicleName.trim().isEmpty()) {
            return vehicleName.trim();
        }
        if (brandVehicleName != null && !brandVehicleName.trim().isEmpty()) {
            return brandVehicleName.trim();
        }
        return slug;
    }
}
