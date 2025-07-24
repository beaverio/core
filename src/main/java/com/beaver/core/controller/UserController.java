package com.beaver.core.controller;

import com.beaver.core.entity.User;
import com.beaver.core.repository.IUserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class UserController {

    private final IUserRepository userRepository;

    public UserController(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user){
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        var u = userRepository.findByEmail(user.getEmail());

        if (!Objects.isNull(u)) {
            return "success";
        }

        return "failure";
    }
}
