package io.github.adnansaliq.workmyresume.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String,String> health(){

        Map<String,String> response = new HashMap<>();
        response.put("status","UP");
        return response;
    }

    @GetMapping("/ping")
    public String ping(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        return "pong!";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        return "Admin Only";
    }
    
}
