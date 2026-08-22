package com.cargosetu.service;

import com.cargosetu.customer.entity.Charges;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.ExtraCharges;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.dto.QuotationDTO;
import com.cargosetu.model.Company;
import com.cargosetu.model.CompanyAsset;
import com.cargosetu.model.Template;
import com.cargosetu.repo.*;
import com.cargosetu.utils.utillity.FileUtility;
import com.cargosetu.utils.utillity.PdfGenerator;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;



@Service
@RequiredArgsConstructor
public class TemplateService {
    private final ShiftingRepository shiftingRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAssetRepository assetRepository;
    private final TemplateRepository templateRepository;
    private final CompanyService companyService;
    @Autowired
    private ClientServiceRepo clientServiceRepo;
    @Autowired
    private PdfGenerator pdfGenerator;

    public void createTemplate(MultipartFile file, Template template) {
        List<String> filePath = FileUtility.saveMultipartFile(new MultipartFile[]{file}, clientServiceRepo.findByName("Template").orElseThrow().getName());

        if (filePath != null && filePath.size() > 0) {
            template.setFilePath(filePath.get(0));
        }
        templateRepository.save(template);
    }

    public byte[] generateQuotationPdf(Long shiftingId) throws Exception {
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
                .tokenMoney(null)
                .build();

        Template tmpl = templateRepository.findByType("QUOTATION").orElseThrow(() -> new RuntimeException("No active QUOTATION template in DB"));
        String htmlContent;
        try {
            if (tmpl.getFilePath() != null && !tmpl.getFilePath().isEmpty()) {
                htmlContent = Files.readString(Path.of(tmpl.getFilePath()));
            } else {
                htmlContent = tmpl.getHtmlContent();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read template file at: " + tmpl.getFilePath(), e);
        }
        String filledHtml = renderHtml(htmlContent, companyDTO, shifting, customer, charges, quotationDTO);

        byte[] bytes = pdfGenerator.generatePdf(filledHtml);
        return bytes;
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
                    htmlContent = Files.readString(Path.of(tmpl.getFilePath()));
                } else {
                    htmlContent = tmpl.getHtmlContent();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read template file at: " + tmpl.getFilePath(), e);
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

}
