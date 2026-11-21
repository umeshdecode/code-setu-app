package com.cargosetu.service;

import com.cargosetu.dto.TruckOptionsDto;

import java.util.List;

public interface TruckOptionService {

    List<TruckOptionsDto> getPublicTruckOptions(String truckType);

    List<TruckOptionsDto> getAllTruckOptions();

    TruckOptionsDto getTruckOptionById(Long id);

    TruckOptionsDto createTruckOption(TruckOptionsDto dto);

    TruckOptionsDto updateTruckOption(Long id, TruckOptionsDto dto);

    void deleteTruckOption(Long id);

    void initDefaultTruckOptionsIfEmpty();
}
