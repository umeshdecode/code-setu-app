package com.cargosetu.mapper.base;

import org.mapstruct.MappingTarget;

public interface BaseUpdateMapper<D,E> {
    // CREATE
    E toEntity(D dto);

    // READ
    D toDto(E entity);

    void updateFromDto(D dto, @MappingTarget E entity);
}
