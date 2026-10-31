package com.cargosetu.notification.service;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateRenderingServiceImpl implements TemplateRenderingService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String EMAIL_LAYOUT_TEMPLATE = "email/layout";
    private static final String EVENT_FILE_TEMPLATES_PREFIX = "templates/email/events/";
    private static final String EVENT_FALLBACK_TEMPLATE = "fallback";

    private final SpringTemplateEngine inlineFragmentEngine = buildInlineFragmentEngine();
    private final SpringTemplateEngine layoutEngine = buildLayoutEngine();
    private final SpringTemplateEngine eventFileEngine = buildEventFileEngine();

    @Override
    public String render(String template, Map<String, Object> variables, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL) {
            return renderHtmlEmail(template, variables);
        }
        return renderPlaceholders(template, variables);
    }

    @Override
    @Deprecated
    public String render(String template, Map<String, Object> variables) {
        return renderPlaceholders(template, variables);
    }

    @Override
    public String renderSubject(String template, Map<String, Object> variables, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL) {
            return renderEmailFragment(template, variables);
        }
        return renderPlaceholders(template, variables);
    }

    /**
     * Resolves {{var}} placeholders then runs the inline Thymeleaf engine, WITHOUT
     * wrapping in the branded page layout. Used for EMAIL subjects.
     */
    private String renderEmailFragment(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        String withPlaceholdersResolved = renderPlaceholders(template, variables);

        Context fragmentContext = new Context();
        if (variables != null) {
            fragmentContext.setVariables(variables);
        }
        return inlineFragmentEngine.process(withPlaceholdersResolved, fragmentContext);
    }

    private String renderHtmlEmail(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }

        // Back-compat: substitute legacy {{var}} placeholders first, so every existing
        // EMAIL template keeps working unchanged. Templates authored with real Thymeleaf
        // syntax (${...}, th:if, th:each, ...) are then processed normally below.
        String renderedContent = renderEmailFragment(template, variables);
        return wrapInLayout(renderedContent, variables);
    }

    private String wrapInLayout(String renderedContent, Map<String, Object> variables) {
        Context layoutContext = new Context();
        layoutContext.setVariable("content", renderedContent);
        layoutContext.setVariable("companyName", "CargoSetu");
        layoutContext.setVariable("currentYear", Year.now().getValue());
        if (variables != null) {
            layoutContext.setVariables(variables);
        }
        // content/companyName/currentYear always win over caller-supplied variables of the same name
        layoutContext.setVariable("content", renderedContent);

        return layoutEngine.process(EMAIL_LAYOUT_TEMPLATE, layoutContext);
    }

    @Override
    public String renderEventFileTemplate(NotificationEventType eventType, Map<String, Object> variables) {
        String eventFileName = eventType.name().toLowerCase();
        boolean eventFileExists = getClass().getClassLoader()
                .getResource(EVENT_FILE_TEMPLATES_PREFIX + eventFileName + ".html") != null;
        String templateToUse = eventFileExists ? eventFileName : EVENT_FALLBACK_TEMPLATE;

        if (!eventFileExists) {
            log.warn("No per-event EMAIL file template found for event: {} — using the common fallback template", eventType);
        }

        Context ctx = new Context();
        if (variables != null) {
            ctx.setVariables(variables);
        }
        ctx.setVariable("eventType", eventType.name());
        ctx.setVariable("variables", variables);

        String renderedContent = eventFileEngine.process(templateToUse, ctx);
        return wrapInLayout(renderedContent, variables);
    }

    private String renderPlaceholders(String template, Map<String, Object> variables) {
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

    private SpringTemplateEngine buildInlineFragmentEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private SpringTemplateEngine buildLayoutEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private SpringTemplateEngine buildEventFileEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(EVENT_FILE_TEMPLATES_PREFIX);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
