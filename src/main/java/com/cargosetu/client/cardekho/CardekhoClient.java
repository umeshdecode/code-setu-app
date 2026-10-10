package com.cargosetu.client.cardekho;

import java.util.List;

public interface CardekhoClient {
    List<CardekhoModelDto> getModelsByBrand(String brandSlug);
}
