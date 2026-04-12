package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.document.model.DocumentUpload;
import com.cargosetu.dto.DocumentUploadDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface DocumentUploadMapper extends BaseUpdateMapper<DocumentUploadDTO, DocumentUpload> {

    @Override
    @Mapping(source = "staff.staffId", target = "staffId")
    @Mapping(source = "document.documentId", target = "documentId")
//    @Mapping(source = "customer.customerId", target = "customerId")
    DocumentUploadDTO toDto(DocumentUpload entity);

    @Override
    @Mapping(source = "staffId", target = "staff.staffId")
    @Mapping(source = "documentId", target = "document.documentId")
//    @Mapping(source = "customerId", target = "customer.customerId")
    DocumentUpload toEntity(DocumentUploadDTO dto);

    @Override
    @InheritConfiguration(name = "toEntity")
    void updateFromDto(DocumentUploadDTO dto, @MappingTarget DocumentUpload entity);

    default Customer mapCustomer(Long customerId) {
        if (customerId == null || customerId == 0) {
            return null;
        }
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        return customer;
    }
}