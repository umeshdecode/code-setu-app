package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.WalletTransactionResponse;
import com.cargosetu.marketplace.entity.WalletTransaction;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface WalletTransactionMapper {
    WalletTransactionResponse toResponse(WalletTransaction entity);

    default List<WalletTransactionResponse> toResponseList(List<WalletTransaction> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }
}
