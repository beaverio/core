package com.beaver.core.user;

import com.beaver.core.user.dto.UpdateSelfDto;
import com.beaver.core.user.dto.UserDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/self", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto getSelf(Authentication authentication) {
        String email = authentication.getName();

        return userService.findByEmail(email)
                .map(UserDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

//    @PatchMapping(value = "/self", produces = MediaType.APPLICATION_JSON_VALUE)
//    public UserDto updateSelf(
//            Authentication authentication,
//            @RequestBody UpdateSelfDto updateProfileRequest) {
//
//    }
}
