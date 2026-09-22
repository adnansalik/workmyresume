package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.service.SubscriptionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final SubscriptionService subscriptionService;

    public AdminController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/upgrade")
    public String upgradeUser(@RequestParam String email) {
        subscriptionService.activate(email, Plan.PRO);
        return "Upgraded to PRO";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/cancel")
    public String cancelSubscription(@RequestParam String email) {
        subscriptionService.cancel(email);
        return "Subscription canceled";
    }
}