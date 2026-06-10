package com.cargosetu.notification.service;

import java.util.Map;

public interface TemplateRenderingService {
    
    /**
     * Renders the given template string by replacing placeholders like {{name}}
     * with the values provided in the variables map.
     *
     * @param template  the template text with placeholders
     * @param variables the map containing key-value pairs for substitution
     * @return the rendered text, with all resolved placeholders replaced
     */
    String render(String template, Map<String, Object> variables);
}
