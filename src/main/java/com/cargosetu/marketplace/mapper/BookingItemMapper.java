package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.BookingItemResponse;
import com.cargosetu.marketplace.entity.BookingItem;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface BookingItemMapper {
    BookingItemResponse toResponse(BookingItem entity);

    default List<BookingItemResponse> toResponseList(List<BookingItem> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }
}
