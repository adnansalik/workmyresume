package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.exception.EmailAlreadyExistsException;
import io.github.adnansaliq.workmyresume.exception.EmailNotExistsException;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public User registerUser(String email,String password) {
        boolean isExistsByEmail = userRepository.existsByEmail(email);
        if(isExistsByEmail){
            throw new EmailAlreadyExistsException("Email already exists ");
        }
        String hashedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(hashedPassword);

        userRepository.save(user);
        return user;

    }

}
