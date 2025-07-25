package com.beaver.core.user;

import com.beaver.core.user.dto.UpdateSelf;
import com.beaver.core.user.mapper.IUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final IUserRepository userRepository;
    private final IUserMapper userMapper;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User getUserSelf(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateSelf(String email, UpdateSelf updateRequest) {
        User existingUser = findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userMapper.mapToEntity(updateRequest, existingUser);
        return userRepository.save(existingUser);
    }

    public void deleteUser(String email) {
        User existingUser = findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(existingUser);
    }
}
