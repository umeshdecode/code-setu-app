package com.cargosetu.client.trucksfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class LenientBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()
                || text.equalsIgnoreCase("null")
                || text.equalsIgnoreCase("not available")
                || text.equalsIgnoreCase("variable")
                || text.equalsIgnoreCase("n/a")
                || text.equalsIgnoreCase("na")) {
            return null;
        }
        try {
            // Remove currency symbols (e.g. ₹, Rs, commas)
            String cleaned = text.replaceAll("[^0-9.]", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
