package com.cargosetu.service;

import com.cargosetu.customer.entity.Charges;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.dto.QuotationDTO;
import com.cargosetu.model.Company;
import com.cargosetu.model.CompanyAsset;
import com.cargosetu.model.Template;
import com.cargosetu.repo.CompanyAssetRepository;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.TemplateRepository;
import com.cargosetu.utils.utillity.FileUtility;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

import static com.cargosetu.document.service.DocumentUploadService.BASE_FILE_URL;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final ShiftingRepository shiftingRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAssetRepository assetRepository;
    private final TemplateRepository templateRepository;
    private final CompanyService companyService;

    public void createTemplate(MultipartFile file, Template template) {
        List<String> filePath = FileUtility.saveMultipartFile(new MultipartFile[]{file}, BASE_FILE_URL);

        if(filePath != null && filePath.size() > 0) {
            template.setFilePath(filePath.get(0));
        }
        templateRepository.save(template);
    }

    public byte[] generateQuotationPdf(Long shiftingId) throws Exception {

        Shifting shifting = shiftingRepository.findById(shiftingId)
                .orElseThrow(() -> new RuntimeException("Shifting not found: " + shiftingId));

        Customer customer = shifting.getCustomer();
        Charges charges  = shifting.getCharge();

        Company company = companyRepository.findById(customer.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found for customer"));

        String logoB64 = companyService.getAssetAsBase64(company.getCompanyId(), "LOGO");
        String sealB64 = companyService.getAssetAsBase64(company.getCompanyId(), "SEAL");

        CompanyDTO companyDTO = CompanyDTO.from(company, logoB64, sealB64);

        double subTotal  = computeSubTotal(charges);
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
            if(tmpl.getFilePath() != null &&  !tmpl.getFilePath().isEmpty()) {
                htmlContent = Files.readString(Path.of(tmpl.getFilePath()));
            }else{
                htmlContent = tmpl.getHtmlContent();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read template file at: " + tmpl.getFilePath(), e);
        }
        String filledHtml = renderHtml(htmlContent, companyDTO, shifting, customer, charges, quotationDTO);

        return htmlToPdf(filledHtml);
    }

    private String renderHtml(String htmlTemplate, CompanyDTO company, Shifting shifting, Customer customer, Charges charges, QuotationDTO quotation) {

        Context ctx = new Context();
        ctx.setVariable("company",company);
        ctx.setVariable("shifting",shifting);
        ctx.setVariable("customer",customer);
        ctx.setVariable("charges",charges);
        ctx.setVariable("quotation",quotation);


        return buildStringEngine().process(htmlTemplate, ctx);
    }

    private SpringTemplateEngine buildStringEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
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

    private double nullSafe(Double v) { return v == null ? 0.0 : v; }
}
