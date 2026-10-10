package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.PaymentTransactionResponse;
import com.cargosetu.marketplace.entity.PaymentTransaction;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentTransactionMapper {
    PaymentTransactionResponse toResponse(PaymentTransaction entity);

    default List<PaymentTransactionResponse> toResponseList(List<PaymentTransaction> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }
}
