package com.eServM.eserv.service;

import com.eServM.eserv.dto.ProductRequest;
import com.eServM.eserv.dto.ProductResponse;
import com.eServM.eserv.exception.BadRequestException;
import com.eServM.eserv.exception.ResourceNotFoundException;
import com.eServM.eserv.model.Product;
import com.eServM.eserv.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findByUid(String uid) {
        return toResponse(fetchProduct(uid));
    }

    public ProductResponse update(String uid, ProductRequest request) {
        Product product = fetchProduct(uid);
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    public void delete(String uid) {
        Product product = fetchProduct(uid);
        productRepository.delete(product);
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }

    private Product fetchProduct(String uid) {
        UUID parsed = parse(uid);
        return productRepository.findByUid(parsed)
                .orElseThrow(() -> new ResourceNotFoundException("未找到商品: " + uid));
    }

    private UUID parse(String uid) {
        try {
            return UUID.fromString(uid);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("无效的UID: " + uid);
        }
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getUid().toString(),
                product.getName(),
                product.getDescription(),
                product.getUnitPrice(),
                product.isActive(),
                product.getCreatedAt());
    }
}
