package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @Autowired
    UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/upgrade")
    public String upgradeUser(@RequestParam String email) {

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPlan(Plan.PRO);
        userRepository.save(user);

        return "Upgraded to PRO";
    }


}
