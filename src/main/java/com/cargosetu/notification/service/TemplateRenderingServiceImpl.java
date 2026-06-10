package com.cargosetu.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateRenderingServiceImpl implements TemplateRenderingService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @Override
    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        if (variables == null) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String placeholderKey = matcher.group(1).trim();
            Object value = variables.get(placeholderKey);
            
            // If variable is missing or null, replace with empty string
            String replacement = value != null ? value.toString() : "";
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
