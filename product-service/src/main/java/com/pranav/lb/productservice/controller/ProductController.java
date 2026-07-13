package com.pranav.lb.productservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProductController {
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @Value("${server.port}")
    private String port;

    private final List<Map<String, Object>> products = List.of(
            Map.of("id", 1, "name", "Mechanical Keyboard", "price", 4999),
            Map.of("id", 2, "name", "27-inch Monitor", "price", 15999),
            Map.of("id", 3, "name", "USB-C Dock", "price", 2999)
    );

    @GetMapping("/products")
    public Map<String, Object> getProducts() {

        return Map.of(
                "products", products,
                "servedByPort", port,
                "instanceId", instanceId,
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable int id) {
        return Map.of(
                "product", products.stream()
                        .filter(p -> p.get("id").equals(id))
                        .findFirst()
                        .orElse(Map.of("error", "not found")),
                "servedByPort", port,
                "instanceId", instanceId
        );
    }
}
