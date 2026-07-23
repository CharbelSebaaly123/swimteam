package com.swimteam.web;

import com.swimteam.dto.MessageResponse;
import com.swimteam.dto.PhotoUploadResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.dto.ProfileUpdateRequest;
import com.swimteam.security.UserPrincipal;
import com.swimteam.service.ProfileService;
import com.swimteam.service.ProfileService.PhotoPayload;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadMyPhoto(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam("file") MultipartFile file) {
        return profileService.uploadMyPhoto(principal, file);
    }

    @GetMapping("/me/photo")
    public ResponseEntity<byte[]> getMyPhoto(@AuthenticationPrincipal UserPrincipal principal) {
        PhotoPayload photo = profileService.getMyPhoto(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.data());
    }

    @DeleteMapping("/me/photo")
    public MessageResponse deleteMyPhoto(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.deleteMyPhoto(principal);
    }
}
