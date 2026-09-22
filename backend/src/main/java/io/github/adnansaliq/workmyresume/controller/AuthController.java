package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.SigninRequest;
import io.github.adnansaliq.workmyresume.dto.Signinresponse;
import io.github.adnansaliq.workmyresume.security.JwtUtil;
import io.github.adnansaliq.workmyresume.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {


    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    AuthController(AuthenticationManager authenticationManager,JwtUtil jwtUtil){
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Signinresponse> login(@Valid @RequestBody SigninRequest request){

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(request.getEmail());

        Signinresponse response = new Signinresponse();
        response.setSuccess(true);
        response.setEmail(userDetails.getUsername());
        response.setToken(token);

        return ResponseEntity.ok(response);
    }
}
