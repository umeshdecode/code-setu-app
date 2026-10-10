package com.cargosetu.client.trucksfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class LenientBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("yes") || text.equalsIgnoreCase("1") || text.equalsIgnoreCase("y")) {
            return true;
        }
        if (text.equalsIgnoreCase("false") || text.equalsIgnoreCase("no") || text.equalsIgnoreCase("0") || text.equalsIgnoreCase("n")) {
            return false;
        }
        return null;
    }
}
