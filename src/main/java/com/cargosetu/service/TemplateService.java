package com.cargosetu.service;

import com.cargosetu.customer.entity.Charges;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.ExtraCharges;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.dto.QuotationDTO;
import com.cargosetu.dto.S3UploadResult;
import com.cargosetu.model.Company;
import com.cargosetu.model.CompanyAsset;
import com.cargosetu.model.CompanyFavouriteTemplate;
import com.cargosetu.model.Template;
import com.cargosetu.repo.*;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.utillity.FileUtility;
import com.cargosetu.utils.utillity.PdfGenerator;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;



@Service
@RequiredArgsConstructor
public class TemplateService {
    private final ShiftingRepository shiftingRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAssetRepository assetRepository;
    private final TemplateRepository templateRepository;
    private final CompanyService companyService;
    private final S3StorageService s3StorageService;
    private final com.cargosetu.notification.infrastructure.channel.EmailChannelSender emailChannelSender;
    private final CompanyFavouriteTemplateRepository companyFavouriteTemplateRepository;
    private final com.cargosetu.shifting.repository.ShiftInventoryItemRepository shiftInventoryItemRepository;

    /** S3 config key — maps to bucket "cargosetu-uploads", folder prefix "documents/" */
    private static final String S3_CONFIG_KEY = "DOCUMENTS";

    @Autowired
    private ClientServiceRepo clientServiceRepo;
    @Autowired
    private PdfGenerator pdfGenerator;

    public void createTemplate(MultipartFile file, Template template) {
        try {
            S3UploadResult result = s3StorageService.uploadFile(file, S3_CONFIG_KEY, "templates/");
            template.setFilePath(result.getS3Key());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload template file to S3", e);
        }
        templateRepository.save(template);
    }

    public Page<Template> getTemplatesByType(String type, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Template> page = templateRepository.findByTypeOrderByIsFavoriteTemplateDesc(type, pageable);

        Integer companyIdInt = ApplicationUtils.getCurrentCompanyId();
        if (companyIdInt != null) {
            Long companyId = companyIdInt.longValue();
            Optional<CompanyFavouriteTemplate> favOpt =
                    companyFavouriteTemplateRepository.findByCompanyIdAndTemplateType(companyId, type);
            if (favOpt.isPresent()) {
                Long favTemplateId = favOpt.get().getTemplateId();
                List<Template> list = new ArrayList<>(page.getContent());
                for (Template t : list) {
                    t.setFavoriteTemplate(Objects.equals(t.getId(), favTemplateId));
                }
                list.sort((a, b) -> Boolean.compare(b.isFavoriteTemplate(), a.isFavoriteTemplate()));
                return new PageImpl<>(list, pageable, page.getTotalElements());
            }
        }
        return page;
    }

    public void setDefaultTemplate(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        Integer companyIdInt = ApplicationUtils.getCurrentCompanyId();
        if (companyIdInt != null) {
            Long companyId = companyIdInt.longValue();
            Optional<CompanyFavouriteTemplate> existingFav =
                    companyFavouriteTemplateRepository.findByCompanyIdAndTemplateType(companyId, template.getType());

            CompanyFavouriteTemplate fav = existingFav.orElseGet(() -> CompanyFavouriteTemplate.builder()
                    .companyId(companyId)
                    .templateType(template.getType())
                    .createdBy(ApplicationUtils.getCurrentUser() != null ? ApplicationUtils.getCurrentUser().getStaffId() : null)
                    .build());

            fav.setTemplateId(templateId);
            fav.setUpdatedAt(LocalDateTime.now());
            companyFavouriteTemplateRepository.save(fav);
        } else {
            // Unset all favorites of this type
            Page<Template> allOfType = templateRepository.findByTypeOrderByIsFavoriteTemplateDesc(
                    template.getType(), PageRequest.of(0, 1000));
            allOfType.getContent().forEach(t -> {
                if (t.isFavoriteTemplate()) {
                    t.setFavoriteTemplate(false);
                    templateRepository.save(t);
                }
            });
            // Set the chosen one
            template.setFavoriteTemplate(true);
            templateRepository.save(template);
        }
    }

    public String getTemplateHtml(Long templateId) {
        Template tmpl = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        try {
            if (tmpl.getFilePath() != null && !tmpl.getFilePath().isEmpty()) {
                byte[] fileBytes = s3StorageService.downloadFile(tmpl.getFilePath(), S3_CONFIG_KEY);
                return new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                return tmpl.getHtmlContent();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read template file from S3: " + tmpl.getFilePath(), e);
        }
    }

    public byte[] generateQuotationPdf(Long shiftingId, Long templateId) throws Exception {
        Shifting shifting = shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)
                .orElseThrow(() -> new RuntimeException("Shifting not found: " + shiftingId));

        Customer customer = shifting.getCustomer();
        Charges charges = shifting.getCharge();

        Company company = companyRepository.findById(customer.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found for customer"));

        String logoB64 = companyService.getAssetAsBase64(company.getCompanyId(), "LOGO");
        String sealB64 = companyService.getAssetAsBase64(company.getCompanyId(), "SEAL");

        CompanyDTO companyDTO = CompanyDTO.from(company, logoB64, sealB64);

        double subTotal = computeSubTotal(charges);
        double gstAmount = nullSafe(charges.getGstCharge());
        double grandTotal = subTotal + gstAmount;

        QuotationDTO quotationDTO = QuotationDTO.builder()
                .quoteDate(LocalDate.now())
                .subTotal(subTotal)
                .grandTotal(grandTotal)
                .tokenMoney(shifting.getTokenMoney())
                .build();

        Template tmpl;
        if (templateId != null) {
            tmpl = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        } else {
            Long compId = customer.getCompanyId() != null ? customer.getCompanyId().longValue() : null;
            Template found = null;
            if (compId != null) {
                Optional<CompanyFavouriteTemplate> fav = companyFavouriteTemplateRepository.findByCompanyIdAndTemplateType(compId, "QUOTATION");
                if (fav.isPresent()) {
                    found = templateRepository.findById(fav.get().getTemplateId()).orElse(null);
                }
            }
            if (found != null) {
                tmpl = found;
            } else {
                tmpl = templateRepository.findByTypeAndIsFavoriteTemplateTrue("QUOTATION")
                        .orElseGet(() -> templateRepository.findByType("QUOTATION")
                                .orElseThrow(() -> new RuntimeException("No active QUOTATION template in DB")));
            }
        }
        String htmlContent;
        try {
            if (tmpl.getFilePath() != null && !tmpl.getFilePath().isEmpty()) {
                byte[] fileBytes = s3StorageService.downloadFile(tmpl.getFilePath(), S3_CONFIG_KEY);
                htmlContent = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                htmlContent = tmpl.getHtmlContent();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read template file from S3: " + tmpl.getFilePath(), e);
        }
        String filledHtml = renderHtml(htmlContent, companyDTO, shifting, customer, charges, quotationDTO);

        byte[] bytes = pdfGenerator.generatePdf(filledHtml);
        return bytes;
    }

    public void generateAndSendQuotationPdf(Long shiftingId, Long templateId) throws Exception {
        byte[] pdf = generateQuotationPdf(shiftingId, templateId);
        
        Shifting shifting = shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)
                .orElseThrow(() -> new RuntimeException("Shifting not found: " + shiftingId));
        com.cargosetu.customer.entity.Customer customer = shifting.getCustomer();
        if (customer == null || customer.getEmail() == null || customer.getEmail().isEmpty()) {
            throw new RuntimeException("Customer email is missing for shifting ID: " + shiftingId);
        }
        
        com.cargosetu.model.Company company = companyRepository.findById(shifting.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
                
        String customerName = (customer.getFirstName() != null ? customer.getFirstName() : "") + 
                              (customer.getLastName() != null ? " " + customer.getLastName() : "");
        customerName = customerName.trim();
        if (customerName.isEmpty()) customerName = "Customer";

        String subject = "Your Quotation from " + company.getCompanyName();
        String body = "Dear " + customerName + ",\n\n"
                    + "Please find attached the quotation for your upcoming shifting from " 
                    + shifting.getShiftFromCity() + " to " + shifting.getShiftToCity() + ".\n\n"
                    + "Thank you for choosing " + company.getCompanyName() + ".";
                    
        String attachmentName = "Quotation-" + shiftingId + ".pdf";
        emailChannelSender.sendEmailWithAttachment(customer.getEmail(), subject, body, pdf, attachmentName);
    }

    private String renderHtml(String htmlTemplate, CompanyDTO company, Shifting shifting, Customer customer, Charges charges, QuotationDTO quotation) {

        Context ctx = new Context();

        Map<String, Object> customerMap = getCustomerMap(customer);

        Map<String, Object> shiftingMap = getShiftingMap(shifting);

        Map<String, Object> chargesMap = getChargesMap(charges);

        ctx.setVariable("company", company);
        ctx.setVariable("customer", customerMap);
        ctx.setVariable("shifting", shiftingMap);
        ctx.setVariable("charges", chargesMap);
        ctx.setVariable("quotation", quotation);
        ctx.setVariable("extras", charges.getExtraCharges());


        return buildStringEngine().process(htmlTemplate, ctx);
    }

    private String renderHtml(String htmlTemplate, CompanyDTO company, Shifting shifting, Customer customer, Charges charges, QuotationDTO quotation, Map<String, Object> variableMap) {

        Context ctx = new Context();

        Map<String, Object> customerMap = getCustomerMap(customer);

        Map<String, Object> shiftingMap = getShiftingMap(shifting);

        Map<String, Object> chargesMap = getChargesMap(charges);

        ctx.setVariable("company", company);
        ctx.setVariable("customer", customerMap);
        ctx.setVariable("shifting", shiftingMap);
        ctx.setVariable("charges", chargesMap);
        ctx.setVariable("quotation", quotation);
        ctx.setVariable("extras", charges.getExtraCharges());

        if (Objects.nonNull(variableMap)) {
            for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
                ctx.setVariable(entry.getKey(), entry.getValue());
            }
        }
        return buildStringEngine().process(htmlTemplate, ctx);
    }

    private static Map<String, Object> getChargesMap(Charges charges) {
        Map<String, Object> chargesMap = new HashMap<>();
        chargesMap.put("transportCharge", charges.getTransportCharge());
        chargesMap.put("packingCharge", charges.getPackingCharge());
        chargesMap.put("loadingCharge", charges.getLoadingCharge());
        chargesMap.put("unloadingCharge", charges.getUnloadingCharge());
        chargesMap.put("unpackingCharge", charges.getUnpackingCharge());
        chargesMap.put("carTransportCharge", charges.getCarTransportCharge());
        chargesMap.put("riskInsureCharge", charges.getRiskInsureCharge());
        chargesMap.put("storageCharge", charges.getStorageCharge());
        chargesMap.put("statisticalCharge", charges.getStatisticalCharge());
        chargesMap.put("serviceCharge", charges.getServiceCharge());
        chargesMap.put("serviceRate", charges.getServiceRate());
        chargesMap.put("gstRate", charges.getGstRate());
        chargesMap.put("gstCharge", charges.getGstCharge());
        return chargesMap;
    }

    private static Map<String, Object> getShiftingMap(Shifting shifting) {
        Map<String, Object> shiftingMap = new HashMap<>();
        shiftingMap.put("shiftingId", shifting.getShiftingId());
        shiftingMap.put("fromAddress", shifting.getFromAddress());
        shiftingMap.put("toAddress", shifting.getToAddress());
        shiftingMap.put("fromCity", shifting.getShiftFromCity());
        shiftingMap.put("toCity", shifting.getShiftToCity());
        shiftingMap.put("shiftingStartDate", shifting.getShiftingStartDate() != null
                ? shifting.getShiftingStartDate().toString() : null);
        shiftingMap.put("shiftReachedDate", shifting.getShiftReachedDate() != null
                ? shifting.getShiftReachedDate().toString() : null);
        return shiftingMap;
    }

    private static Map<String, Object> getCustomerMap(Customer customer) {
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("firstName", customer.getFirstName());
        customerMap.put("lastName", customer.getLastName());
        customerMap.put("mobileNo", customer.getMobileNo());
        customerMap.put("workEmail", customer.getWorkEmail());
        customerMap.put("uid", customer.getUuid());
        return customerMap;
    }

    private SpringTemplateEngine buildStringEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Deprecated
    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        }
    }

    private byte[] htmlToPdfConverter(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.run();
            return out.toByteArray();
        }
    }

    private double computeSubTotal(Charges c) {
        double s = 0;
        s += nullSafe(c.getTransportCharge());
        s += nullSafe(c.getPackingCharge());
        s += nullSafe(c.getLoadingCharge());
        s += nullSafe(c.getUnloadingCharge());
        s += nullSafe(c.getUnpackingCharge());
        s += nullSafe(c.getCarTransportCharge());
        s += nullSafe(c.getRiskInsureCharge());
        s += nullSafe(c.getStorageCharge());
        s += nullSafe(c.getStatisticalCharge());
        s += nullSafe(c.getServiceCharge());
        if (c.getExtraCharges() != null)
            for (var ex : c.getExtraCharges()) s += nullSafe(ex.getChargeValue());
        return s;
    }

    private double nullSafe(Double v) {
        return v == null ? 0.0 : v;
    }

    public byte[] generateInvoiceHtml(Long shiftingId) throws Exception {
        try {
            Shifting shifting = shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)
                    .orElseThrow(() -> new RuntimeException("Shifting not found: " + shiftingId));

            Customer customer = shifting.getCustomer();
            Charges charges = shifting.getCharge();

            Company company = companyRepository.findById(customer.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found for customer"));

            String logoB64 = companyService.getAssetAsBase64(company.getCompanyId(), "LOGO");
            String sealB64 = companyService.getAssetAsBase64(company.getCompanyId(), "SEAL");

            CompanyDTO companyDTO = CompanyDTO.from(company, logoB64, sealB64);

            double subTotal = computeSubTotal(charges);
            double gstAmount = nullSafe(charges.getGstCharge());
            double grandTotal = subTotal + gstAmount;

            QuotationDTO quotation = QuotationDTO.builder()
                    .quoteDate(LocalDate.now())
                    .subTotal(subTotal)
                    .grandTotal(grandTotal)
                    .tokenMoney(null)
                    .build();

            Context ctx = new Context();

            Template tmpl = templateRepository.findByType("INVOICE").orElseThrow(() -> new RuntimeException("No active Invoice template "));
            String htmlContent;
            try {
                if (tmpl.getFilePath() != null && !tmpl.getFilePath().isEmpty()) {
                    byte[] fileBytes = s3StorageService.downloadFile(tmpl.getFilePath(), S3_CONFIG_KEY);
                    htmlContent = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    htmlContent = tmpl.getHtmlContent();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read template file from S3: " + tmpl.getFilePath(), e);
            }
            Map<String, Object> variableMap = new HashMap<>();
            variableMap.put("invoiceNo", "INV-" + shiftingId + "-2026");
            variableMap.put("invoiceDate", LocalDate.now());
            variableMap.put("dueDate", LocalDate.now().plusDays(7));
            variableMap.put("subTotal", subTotal);
            variableMap.put("grandTotal", grandTotal);
            String filledHtml = renderHtml(htmlContent, companyDTO, shifting, customer, charges, quotation, variableMap);

            byte[] bytes = pdfGenerator.generatePdf(filledHtml);
            return bytes;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] generatePackingListPdf(Long shiftingId, Long templateId) throws Exception {
        Shifting shifting = shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)
                .orElseThrow(() -> new RuntimeException("Shifting not found"));

        Customer customer = shifting.getCustomer();
        Company company = companyRepository.findById(shifting.getCompanyId() != null ? shifting.getCompanyId() : customer.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found for customer"));

        String logoB64 = companyService.getAssetAsBase64(company.getCompanyId(), "LOGO");
        String sealB64 = companyService.getAssetAsBase64(company.getCompanyId(), "SEAL");

        CompanyDTO companyDTO = CompanyDTO.from(company, logoB64, sealB64);
        List<com.cargosetu.shifting.entity.ShiftInventoryItem> items = shiftInventoryItemRepository.findByShifting_ShiftingIdOrderByIdAsc(shiftingId);

        Template tmpl = null;
        if (templateId != null) {
            tmpl = templateRepository.findById(templateId).orElse(null);
        } else {
            Long compId = company.getCompanyId() != null ? company.getCompanyId().longValue() : null;
            if (compId != null) {
                Optional<CompanyFavouriteTemplate> fav = companyFavouriteTemplateRepository.findByCompanyIdAndTemplateType(compId, "PACKING_LIST");
                if (fav.isPresent()) {
                    tmpl = templateRepository.findById(fav.get().getTemplateId()).orElse(null);
                }
            }
            if (tmpl == null) {
                tmpl = templateRepository.findByTypeAndIsFavoriteTemplateTrue("PACKING_LIST")
                        .orElseGet(() -> templateRepository.findByType("PACKING_LIST").orElse(null));
            }
        }

        String htmlContent;
        if (tmpl != null) {
            try {
                if (tmpl.getFilePath() != null && !tmpl.getFilePath().isEmpty()) {
                    byte[] fileBytes = s3StorageService.downloadFile(tmpl.getFilePath(), S3_CONFIG_KEY);
                    htmlContent = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    htmlContent = tmpl.getHtmlContent();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read template file from S3: " + tmpl.getFilePath(), e);
            }
        } else {
            htmlContent = getDefaultPackingListTemplateHtml();
        }

        Context ctx = new Context();
        ctx.setVariable("company", companyDTO);
        ctx.setVariable("customer", getCustomerMap(customer));
        ctx.setVariable("shifting", getShiftingMap(shifting));
        ctx.setVariable("items", items);
        ctx.setVariable("totalItems", items.size());

        String filledHtml = buildStringEngine().process(htmlContent, ctx);
        return pdfGenerator.generatePdf(filledHtml);
    }

    public void generateAndSendPackingListPdf(Long shiftingId, Long templateId) throws Exception {
        byte[] pdf = generatePackingListPdf(shiftingId, templateId);

        Shifting shifting = shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)
                .orElseThrow(() -> new RuntimeException("Shifting not found: " + shiftingId));
        Customer customer = shifting.getCustomer();
        if (customer == null || (customer.getWorkEmail() == null && customer.getEmail() == null)) {
            throw new RuntimeException("Customer email is missing for shifting ID: " + shiftingId);
        }

        String recipientEmail = customer.getWorkEmail() != null && !customer.getWorkEmail().isEmpty()
                ? customer.getWorkEmail() : customer.getEmail();

        Company company = companyRepository.findById(shifting.getCompanyId() != null ? shifting.getCompanyId() : customer.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String customerName = (customer.getFirstName() != null ? customer.getFirstName() : "") +
                              (customer.getLastName() != null ? " " + customer.getLastName() : "");
        customerName = customerName.trim();
        if (customerName.isEmpty()) customerName = "Customer";

        String subject = "Packaging List from " + company.getCompanyName();
        String body = "Dear " + customerName + ",\n\n"
                    + "Please find attached the packaging list for your upcoming relocation move from "
                    + shifting.getShiftFromCity() + " to " + shifting.getShiftToCity() + ".\n\n"
                    + "Thank you for choosing " + company.getCompanyName() + ".";

        String attachmentName = "PackingList-" + shiftingId + ".pdf";
        emailChannelSender.sendEmailWithAttachment(recipientEmail, subject, body, pdf, attachmentName);
    }

    private String getDefaultPackingListTemplateHtml() {
        try {
            java.nio.file.Path defaultPath = java.nio.file.Paths.get("C:/Users/Dell/Desktop/Personal/Cargosetu/packing-list-templatev1.html");
            if (java.nio.file.Files.exists(defaultPath)) {
                return new String(java.nio.file.Files.readAllBytes(defaultPath), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "<html><body><h1>Packaging Inventory List</h1></body></html>";
    }

}
