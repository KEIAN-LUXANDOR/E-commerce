package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.common.PageResponse;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> listProducts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) Long minPrice,
            @RequestParam(name = "maxPrice", required = false) Long maxPrice,
            @RequestParam(name = "origin", required = false) DiamondGrades.Origin origin,
            @RequestParam(name = "shape", required = false) List<DiamondGrades.Shape> shape,
            @RequestParam(name = "minCarat", required = false) BigDecimal minCarat,
            @RequestParam(name = "maxCarat", required = false) BigDecimal maxCarat,
            @RequestParam(name = "color", required = false) List<DiamondGrades.Color> color,
            @RequestParam(name = "clarity", required = false) List<DiamondGrades.Clarity> clarity,
            @RequestParam(name = "cut", required = false) List<DiamondGrades.Cut> cut,
            @RequestParam(name = "polish", required = false) List<DiamondGrades.Finish> polish,
            @RequestParam(name = "symmetry", required = false) List<DiamondGrades.Finish> symmetry,
            @RequestParam(name = "fluorescence", required = false) List<DiamondGrades.Fluorescence> fluorescence,
            @RequestParam(name = "lab", required = false) List<DiamondGrades.Lab> lab,
            @RequestParam(name = "withImage", defaultValue = "false") boolean withImage,
            @RequestParam(name = "quickShip", defaultValue = "false") boolean quickShip,
            @RequestParam(name = "sort", defaultValue = "recommended") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        DiamondFilter diamondFilter = new DiamondFilter(origin, shape, minCarat, maxCarat, color, clarity,
                cut, polish, symmetry, fluorescence, lab, withImage, quickShip);

        return productService.searchActiveProducts(q, category, minPrice, maxPrice, diamondFilter, sort, page, size);
    }

    @GetMapping("/{slug}")
    public ProductResponse getProduct(@PathVariable("slug") String slug) {
        return productService.getActiveProduct(slug);
    }
}