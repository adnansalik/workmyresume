package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.SignupRequest;
import io.github.adnansaliq.workmyresume.dto.SignupResponse;
import io.github.adnansaliq.workmyresume.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/users")
    ResponseEntity<SignupResponse> handleSignUp(@Valid @RequestBody SignupRequest request){
        SignupResponse response = new SignupResponse();
        User user = userService.registerUser(request.getEmail(),request.getPassword());
        response.setEmail(user.getEmail());
        response.setIsActive(user.getIsActive());
        response.setMessage("SUCCESS");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
