package com.swimteam.web;

import com.swimteam.dto.ProfileResponse;
import com.swimteam.dto.ProfileUpdateRequest;
import com.swimteam.security.UserPrincipal;
import com.swimteam.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.getMyProfile(principal);
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateMyProfile(principal, request);
    }
}
