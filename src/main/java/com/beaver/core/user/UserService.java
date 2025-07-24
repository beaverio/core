package com.beaver.core.user;

import com.beaver.core.user.dto.UpdateSelfDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final IUserRepository userRepository;

    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void createUser(User user) {
        userRepository.save(user);
    }

//    public User updateUser(UpdateSelfDto user) {
//        return userRepository.save(user);
//    }
}
