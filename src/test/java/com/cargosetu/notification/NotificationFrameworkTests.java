package com.cargosetu.notification;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationTemplateRepository;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.notification.service.TemplateRenderingService;
import com.cargosetu.notification.service.NotificationProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationFrameworkTests {

    @Autowired
    private TemplateRenderingService renderingService;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationAuditRepository auditRepository;

    @Autowired
    private NotificationProcessingService processingService;

    @Test
    void testTemplateRendering_Success() {
        String template = "Hello {{name}}, welcome to {{company}}!";
        Map<String, Object> variables = Map.of(
                "name", "John Doe",
                "company", "CargoSetu"
        );

        String result = renderingService.render(template, variables);
        assertThat(result).isEqualTo("Hello John Doe, welcome to CargoSetu!");
    }

    @Test
    void testTemplateRendering_MissingVariable() {
        String template = "Hello {{name}}, your code is {{code}}.";
        Map<String, Object> variables = Map.of(
                "name", "Alice"
        );

        String result = renderingService.render(template, variables);
        // Missing key should be replaced by empty string
        assertThat(result).isEqualTo("Hello Alice, your code is .");
    }

    @Test
    void testDatabaseInitializer_TemplatesSeeded() {
        Optional<NotificationTemplate> staffTemplate = templateRepository
                .findByTemplateCodeAndChannel("STAFF_CREATED", NotificationChannel.EMAIL);
        
        assertThat(staffTemplate).isPresent();
        assertThat(staffTemplate.get().getSubjectTemplate()).contains("Welcome");
        assertThat(staffTemplate.get().getBodyTemplate()).contains("staffId");
    }

    @Test
    void testNotificationProcessingPipeline() {
        // Prepare template in DB
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode("TEST_PIPELINE_EVENT");
        template.setChannel(NotificationChannel.EMAIL);
        template.setSubjectTemplate("Test Subject: {{subject}}");
        template.setBodyTemplate("Test Body: {{body}}");
        template.setIsActive(true);
        templateRepository.save(template);

        // Prepare generic event
        UUID eventId = UUID.randomUUID();
        Map<String, Object> variables = Map.of(
                "subject", "Hello World",
                "body", "This is a pipeline integration test."
        );
        GenericNotificationEvent event = new GenericNotificationEvent(
                eventId,
                "TEST_PIPELINE_EVENT",
                List.of("test@cargosetu.com"),
                NotificationChannel.EMAIL,
                variables,
                java.time.Instant.now(),
                null
        );

        // Process event
        processingService.processEvent(event);

        // Verify audit record created
        List<NotificationAudit> audits = auditRepository.findByEventId(eventId);
        assertThat(audits).hasSize(1);
        
        NotificationAudit audit = audits.get(0);
        assertThat(audit.getRecipient()).isEqualTo("test@cargosetu.com");
        assertThat(audit.getSubject()).isEqualTo("Test Subject: Hello World");
        assertThat(audit.getMessageBody()).isEqualTo("Test Body: This is a pipeline integration test.");
        
        // Since mock mode is enabled for EMAIL by default in local properties, it should successfully mock-send
        assertThat(audit.getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
