package com.cargosetu.client.trucksfloor;

public interface TrucksfloorClient {
    TrucksfloorResponse getTrucks(String brandSlug, int page);
}
