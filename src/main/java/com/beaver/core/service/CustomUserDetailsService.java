package com.beaver.core.service;

import com.beaver.core.CustomUserDetails;
import com.beaver.core.entity.User;
import com.beaver.core.repository.IUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;

    public CustomUserDetailsService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("Email not found");
        }

        return new CustomUserDetails(user);
    }
}
