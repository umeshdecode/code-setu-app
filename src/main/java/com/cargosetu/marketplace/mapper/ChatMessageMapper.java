package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.ChatMessageRequest;
import com.cargosetu.marketplace.dto.ChatMessageResponse;
import com.cargosetu.marketplace.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ChatMessageMapper {
    ChatMessageResponse toResponse(ChatMessage entity);

    default List<ChatMessageResponse> toResponseList(List<ChatMessage> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderStaffId", ignore = true)
    @Mapping(target = "senderCompanyId", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    ChatMessage toEntity(ChatMessageRequest request);
}
