package com.AiPortal.controller; // Sub-pakke av rotpakken

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
// @CrossOrigin(origins = "http://localhost:5173") // For React senere
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        return Map.of("message", "Hei fra Spring Boot demo1! Ny start!");
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "Backend (demo1) er oppe og kjører!");
    }
}