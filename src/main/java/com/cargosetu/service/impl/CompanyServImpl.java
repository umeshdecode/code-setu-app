package com.cargosetu.service.impl;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.CompanyAssetDTO;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Clientservice;
import com.cargosetu.model.Company;
import com.cargosetu.model.CompanyAsset;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.ClientServiceRepo;
import com.cargosetu.repo.CompanyAssetRepository;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.service.CompanyService;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.UUIDGenerator;
import com.cargosetu.utils.utillity.FileUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import com.cargosetu.service.S3StorageService;
import com.cargosetu.dto.S3UploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class CompanyServImpl implements CompanyService {
    @Autowired
    private  CompanyRepository companyRepository;
    @Autowired
    private CompanyAssetRepository companyAssetRepository;
    @Autowired
    private ClientServiceRepo clientServiceRepo;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private S3StorageService s3StorageService;

    @Override
    public GenericDataDTO createCompany(CompanyDTO companyDto, Long staffId) {
        GenericDataDTO response = new GenericDataDTO();
        String prefix = "COPTIC";
        String suffix = "6293";
        try {
            log.info("Creating company with email {}", companyDto.getCompanyEmail());

            if (companyRepository.existsByCompanyEmail(companyDto.getCompanyEmail())) {
                response.setResponseMessege("Company email already exists");
                response.setResponseCode(409);
                return response;
            }
            if(companyDto.getCompanyName().length() >= 5){
                prefix = companyDto.getCompanyName().substring(0,4).toUpperCase();
            }
            StringBuilder rev = new StringBuilder(companyDto.getContactNo1().toString()).reverse();
            if(!rev.isEmpty()){
                suffix = rev.substring(0,4);
            }
            Company company = new Company(companyDto);
            company.setCompanyUUID(UUIDGenerator.generate(prefix,suffix));
            Company saved = companyRepository.save(company);
            Optional<Staff> staff = staffRepository.findById(staffId);
            if(staff.isPresent()){
                staff.get().setCompanyId(saved.getCompanyId());
                staffRepository.save(staff.get());
            }
            response.setResponseMessege("Company created successfully");
            response.setResponseCode(200);
            response.setData(saved);
           createCompanyAssets(companyDto.getAssets() ,saved.getCompanyId());


        } catch (Exception e) {
            log.error("Error creating company", e);
            response.setResponseMessege("Failed to create company");
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public GenericDataDTO updateCompany(Integer id, CompanyDTO company) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Updating company ID {}", id);

            Company existing = companyRepository.findById(id).orElse(null);
            if (existing == null) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            if (company.getCompanyName() != null) {
                existing.setCompanyName(company.getCompanyName());
            }

            if (company.getCompanyEmail() != null) {
                existing.setCompanyEmail(company.getCompanyEmail());
            }

            if (company.getWhatsappNumber() != null) {
                existing.setWhatsappNumber(company.getWhatsappNumber());
            }

            if (company.getContactNo1() != null) {
                existing.setContactNo1(company.getContactNo1());
            }

            if (company.getContactNo2() != null) {
                existing.setContactNo2(company.getContactNo2());
            }

            if (company.getAddress() != null) {
                existing.setAddress(company.getAddress());
            }

            if (company.getCity() != null) {
                existing.setCity(company.getCity());
            }

            if (company.getState() != null) {
                existing.setState(company.getState());
            }

            if (company.getCountry() != null) {
                existing.setCountry(company.getCountry());
            }
            if(company.getWebsite() != null){
                existing.setWebsite(company.getWebsite());
            }

            existing.setUpdatedAt(LocalDateTime.now());

            Company updated = companyRepository.save(existing);

            // Handle updating of company assets (logos, seals, stamps)
            if (company.getAssets() != null && !company.getAssets().isEmpty()) {
                createCompanyAssets(company.getAssets(), id);
            }

            response.setResponseMessege("Company updated successfully");
            response.setResponseCode(200);
            response.setData(updated);

        } catch (Exception e) {
            log.error("Error updating company {}", id, e);
            response.setResponseMessege("Failed to update company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getCompanyById(String id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching company {}", id);

            Company company = companyRepository.findByCompanyUUIDAndIsDeleted(id, false).orElse(null);
            if (company == null) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            response.setResponseMessege("Company fetched successfully");
            response.setResponseCode(200);
            response.setData(mapToCompanyDTOWithAssets(company));

        } catch (Exception e) {
            log.error("Error fetching company {}", id, e);
            response.setResponseMessege("Failed to fetch company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getAllCompanies(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching all companies");

            if(user.getStaffId() == 1){
                List<Company> list = companyRepository.findAll();
                response.setResponseMessege("Companies fetched successfully");
                response.setResponseCode(200);
                response.setDataList(list);
            }else {
                if(user.getCompanyId() != null){
                    Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
                    if (company == null) {
                        response.setResponseMessege("Company not found");
                        response.setResponseCode(404);
                        return response;
                    }

                    response.setResponseMessege("Company fetched successfully");
                    response.setResponseCode(200);
                    response.setDataList(List.of(company));
                }else{
                    response.setResponseMessege("Company not found");
                    response.setResponseCode(404);
                    return response;
                }
            }

            return response;

        } catch (Exception e) {
            log.error("Error fetching companies", e);
            response.setResponseMessege("Failed to fetch companies");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO deleteCompany(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Deleting company {}", id);

            if (!companyRepository.existsById(id)) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            Optional<Company> company = companyRepository.findById(id);
            if(company.isPresent()){
                company.get().setIsActive(false);
                company.get().setIsDeleted(true);
                companyRepository.save(company.get());
            }
            response.setResponseMessege("Company deleted successfully");
            response.setResponseCode(200);

        } catch (Exception e) {
            log.error("Error deleting company {}", id, e);
            response.setResponseMessege("Failed to delete company");
            response.setResponseCode(500);
        }
        return response;
    }


    private void validateCompanyAssetFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("Files are required");
        }
        List<String> allowedExtensions = List.of(
                "pdf",
                "jpg",
                "jpeg",
                "png",
                "svg"
        );
        long maxFileSize = 1024 * 1024;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Invalid file");
            }
            if (file.getSize() > maxFileSize) {
                throw new RuntimeException(
                        "File size must not exceed 1 MB"
                );
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw new RuntimeException("File name is invalid");
            }
            String extension = "";
            int lastIndex = fileName.lastIndexOf(".");
            if (lastIndex > 0) {
                extension = fileName.substring(lastIndex + 1).toLowerCase();
            }
            if (!allowedExtensions.contains(extension)) {
                throw new RuntimeException(
                        "Only pdf, jpg, jpeg, png and svg files are allowed"
                );
            }
        }
    }

    @Override
    @Transactional
    public void createCompanyAssets(List<CompanyAssetDTO> companyAssets, Integer companyId) {
        if (companyAssets == null || companyAssets.isEmpty()) {
            return;
        }

        for (CompanyAssetDTO dto : companyAssets) {
            if (dto == null) {
                continue;
            }
            MultipartFile[] files = dto.getFile();
            if (files == null || files.length == 0) {
                continue;
            }
            MultipartFile file = files[0];
            if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
                continue;
            }

            validateCompanyAssetFiles(files);

            try {
                // Upload asset to S3 (using "COMPANY_ASSETS" bucket config)
                S3UploadResult uploadResult = s3StorageService.uploadFile(file, "COMPANY_ASSETS");
                String s3Key = uploadResult.getS3Key();

                // Check if asset already exists for this company and asset type
                CompanyAsset existingAsset = companyAssetRepository.findByCompanyIdAndAssetType(companyId, dto.getAssetType());
                if (existingAsset != null) {
                    String oldFilePath = existingAsset.getFilePath();
                    if (oldFilePath != null && !oldFilePath.isBlank()) {
                        try {
                            s3StorageService.deleteFile(oldFilePath, "COMPANY_ASSETS");
                            log.info("Deleted old company asset from S3: {}", oldFilePath);
                        } catch (Exception ex) {
                            log.warn("Failed to delete old company asset from S3: {}", oldFilePath, ex);
                        }
                    }
                    existingAsset.setFilePath(s3Key);
                    companyAssetRepository.save(existingAsset);
                    log.info("Updated company asset {} for companyId={}", dto.getAssetType(), companyId);
                } else {
                    CompanyAsset newAsset = new CompanyAsset();
                    newAsset.setCompanyId(companyId);
                    newAsset.setAssetType(dto.getAssetType());
                    newAsset.setFilePath(s3Key);
                    companyAssetRepository.save(newAsset);
                    log.info("Created new company asset {} for companyId={}", dto.getAssetType(), companyId);
                }
            } catch (Exception e) {
                log.error("Failed to upload company asset {} to S3", dto.getAssetType(), e);
                throw new RuntimeException("Asset upload to S3 failed: " + e.getMessage(), e);
            }
        }
    }

    public CompanyAsset convertCompanyAssetDtoToEntity(CompanyAssetDTO dto) {
        CompanyAsset companyAsset = new CompanyAsset();
        companyAsset.setAssetType(dto.getAssetType());
        companyAsset.setFilePath(dto.getFilePath());
        companyAsset.setCompanyId(dto.getCompanyId());
        companyAsset.setId(dto.getId());
        return companyAsset;
    }

    /**
     * Fetches the asset record, reads the file from local disk or downloads from S3,
     * and returns a ready-to-use base64 data URI.
     * Returns null if no asset found or file cannot be read.
     */
    @Override
    public String getAssetAsBase64(Integer companyId, String assetType) {
        Optional<CompanyAsset> opt = Optional.ofNullable(companyAssetRepository.findByCompanyIdAndAssetType(companyId, assetType));

        if (opt.isEmpty()) {
            log.warn("No asset found for companyId={} type={}", companyId, assetType);
            return null;
        }

        String filePath = opt.get().getFilePath();
        if (filePath == null || filePath.isBlank()) {
            log.warn("Asset record exists but filePath is empty for companyId={} type={}", companyId, assetType);
            return null;
        }

        try {
            byte[] bytes;
            // Check if it's a local file path
            Path localPath = Path.of(filePath);
            if (Files.exists(localPath)) {
                bytes = Files.readAllBytes(localPath);
            } else {
                // Otherwise retrieve from S3
                bytes = s3StorageService.downloadFile(filePath, "COMPANY_ASSETS");
            }

            String b64 = Base64.getEncoder().encodeToString(bytes);
            String mime = detectMimeType(filePath);
            return "data:" + mime + ";base64," + b64;

        } catch (Exception e) {
            log.error("Failed to read asset from local disk or S3 path={}: {}", filePath, e.getMessage());
            return null;   // template handles null → shows placeholder box
        }
    }


    @Override
    public GenericDataDTO getCompany(Integer companyId) {
        GenericDataDTO response = new GenericDataDTO();
        try {

            Company company = companyRepository.findById(companyId).orElse(null);
            if (company == null) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            response.setResponseMessege("Company fetched successfully");
            response.setResponseCode(200);
            response.setData(mapToCompanyDTOWithAssets(company));

        } catch (Exception e) {
            response.setResponseMessege("Failed to fetch company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getCompanyByStaffId(Long staffId) {

        log.info("Fetching company details for staffId: {}", staffId);

        GenericDataDTO response = new GenericDataDTO();

        try {
            Company company = companyRepository.findCompanyByStaffId(staffId).orElseThrow(() -> {
                        log.warn("Company not found for staffId: {}", staffId);

                        return new ResourceNotFoundException(
                                "Company",
                                "staffId",
                                new Exception()
                        );
                    });

            response.setData(mapToCompanyDTOWithAssets(company));
            response.setResponseCode(HttpStatus.OK.value());
            response.setResponseMessege("Company details fetched successfully.");

            log.info("Company details fetched successfully. CompanyId: {}, staffId: {}",
                    company.getCompanyId(),
                    staffId);

        } catch (ResourceNotFoundException ex) {

            response.setResponseCode(HttpStatus.NO_CONTENT.value());
            response.setResponseMessege(ex.getMessage());

            log.warn("No company found for staffId: {}", staffId);

        } catch (Exception ex) {

            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setResponseMessege("Failed to fetch company details.");

            log.error("Unexpected error while fetching company details for staffId: {}",
                    staffId,
                    ex);
        }

        return response;
    }

    private CompanyDTO mapToCompanyDTOWithAssets(Company company) {
        String name = company.getCompanyName() != null ? company.getCompanyName().trim() : "";
        int spaceIdx = name.indexOf(' ');
        String first = spaceIdx > 0 ? name.substring(0, spaceIdx) : name;
        String rest  = spaceIdx > 0 ? name.substring(spaceIdx)    : "";

        CompanyDTO dto = CompanyDTO.builder()
                .companyId(company.getCompanyId())
                .companyName(name)
                .companyNameFirst(first)
                .companyNameRest(rest)
                .companyEmail(company.getCompanyEmail())
                .whatsappNumber(company.getWhatsappNumber())
                .contactNo1(company.getContactNo1())
                .contactNo2(company.getContactNo2())
                .landlineNo(company.getLandlineNo())
                .tollFreeNumber(company.getTollFreeNumber())
                .panNumber(company.getPanNumber())
                .gstNumber(company.getGstNumber())
                .address(company.getAddress())
                .country(company.getCountry())
                .state(company.getState())
                .city(company.getCity())
                .branch(company.getBranch())
                .mainBranch(company.getMainBranch())
                .companyUUID(company.getCompanyUUID())
                .website(company.getWebsite())
                .isActive(company.getIsActive())
                .build();
                dto.setActive(company.getIsActive());
        List<CompanyAsset> assets = companyAssetRepository.findByCompanyId(company.getCompanyId());
        if (assets != null && !assets.isEmpty()) {
            List<CompanyAssetDTO> assetDTOs = new ArrayList<>();
            for (CompanyAsset asset : assets) {
                CompanyAssetDTO aDto = new CompanyAssetDTO();
                aDto.setId(asset.getId());
                aDto.setCompanyId(asset.getCompanyId());
                aDto.setAssetType(asset.getAssetType());
                aDto.setFilePath(asset.getFilePath());
                
                String filePath = asset.getFilePath();
                if (filePath != null && !filePath.isBlank()) {
                    try {
                        Path localPath = Path.of(filePath);
                        if (!Files.exists(localPath)) {
                            String url = s3StorageService.getViewableUrl(filePath, "COMPANY_ASSETS");
                            aDto.setFileUrl(url);
                        }
                    } catch (Exception e) {
                        log.warn("Could not generate URL for asset: {}", e.getMessage());
                    }
                }
                assetDTOs.add(aDto);
            }
            dto.setAssets(assetDTOs);
        }
        return dto;
    }

    /** Infer MIME type from file extension. */
    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        return "image/jpeg";   // safe default
    }
}
