package com.keianluxandor.backend.catalog;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final VariantStockRepository stockRepository;

    public StockService(VariantStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // true if one piece was reserved; false if it's already sold
    @Transactional
    public boolean reserveOne(UUID variantId) {
        return stockRepository.takeOne(variantId) == 1;
    }

    @Transactional
    public void releaseOne(UUID variantId) {
        stockRepository.putBackOne(variantId);
    }
}