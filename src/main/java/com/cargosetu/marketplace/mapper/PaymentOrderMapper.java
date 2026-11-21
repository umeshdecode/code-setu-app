package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.CreatePaymentOrderRequest;
import com.cargosetu.marketplace.dto.PaymentOrderResponse;
import com.cargosetu.marketplace.entity.PaymentOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentOrderMapper {
    PaymentOrderResponse toResponse(PaymentOrder entity);

    default List<PaymentOrderResponse> toResponseList(List<PaymentOrder> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "payerCompanyId", ignore = true)
    @Mapping(target = "payeeCompanyId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "providerRef", ignore = true)
    @Mapping(target = "createdByStaffId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "dueAt", ignore = true)
    PaymentOrder toEntity(CreatePaymentOrderRequest request);
}
