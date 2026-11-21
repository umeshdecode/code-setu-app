package com.cargosetu.client.trucksfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class LenientIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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
            String cleaned = text.replace(",", "").trim();
            if (cleaned.contains(".")) {
                return (int) Math.round(Double.parseDouble(cleaned));
            }
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
