package com.pranav.lb.orderservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;
    private static final String PRODUCT_SERVICE_URL = "http://product-service/products";

    @GetMapping("/order/place")
    public Map<String, Object> placeOrder() {
        Map<?, ?> productResponse = restTemplate.getForObject(PRODUCT_SERVICE_URL, Map.class);

        Map<String, Object> orderResult = new HashMap<>();
        orderResult.put("orderStatus", "CONFIRMED");
        orderResult.put("calledUrl", PRODUCT_SERVICE_URL);
        orderResult.put("productServiceResponse", productResponse);
        return orderResult;
    }

    @GetMapping("/order/stress-test")
    public Map<String, Object> stressTest() {

        Map<String, Object> results = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            Map<?, ?> response = restTemplate.getForObject(PRODUCT_SERVICE_URL, Map.class);
            Object port = response != null ? response.get("servedByPort") : "N/A";
            results.put("call-" + i, "servedByPort=" + port);
        }
        return results;
    }
}
