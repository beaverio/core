package com.beaver.core.user;

import com.beaver.core.user.dto.UpdateSelf;
import com.beaver.core.user.dto.UserDto;
import jakarta.validation.Valid;
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
        User user = userService.getUserSelf(email);
        return UserDto.fromEntity(user);
    }

    @PatchMapping(value = "/self", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto updateSelf(
            Authentication authentication,
            @Valid @RequestBody UpdateSelf updateProfileRequest)
    {
        String email = authentication.getName();
        User user = userService.updateSelf(email, updateProfileRequest);
        return UserDto.fromEntity(user);
    }
}
