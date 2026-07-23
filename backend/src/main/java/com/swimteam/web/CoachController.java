package com.swimteam.web;

import com.swimteam.dto.AgeGroupReportResponse;
import com.swimteam.dto.CoachMetricsResponse;
import com.swimteam.dto.CoachProfileResponse;
import com.swimteam.dto.CoachProfileUpdateRequest;
import com.swimteam.dto.MemberSummaryResponse;
import com.swimteam.dto.PhotoUploadResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.security.UserPrincipal;
import com.swimteam.service.ProfileService;
import com.swimteam.service.ProfileService.PhotoPayload;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final ProfileService profileService;

    public CoachController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public CoachProfileResponse getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.getCoachProfile(principal);
    }

    @PutMapping("/me")
    public CoachProfileResponse updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CoachProfileUpdateRequest request) {
        return profileService.updateCoachProfile(principal, request);
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadMyPhoto(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam("file") MultipartFile file) {
        return profileService.uploadCoachPhoto(principal, file);
    }

    @GetMapping("/me/photo")
    public ResponseEntity<byte[]> getMyPhoto(@AuthenticationPrincipal UserPrincipal principal) {
        PhotoPayload photo = profileService.getCoachPhoto(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.data());
    }

    @GetMapping("/members")
    public List<MemberSummaryResponse> listMembers(
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return profileService.listMembersForCoach(sort, direction);
    }

    @GetMapping("/members/{userId}")
    public ProfileResponse getMember(@PathVariable Long userId) {
        return profileService.getMemberProfileForCoach(userId);
    }

    @GetMapping("/members/{userId}/photo")
    public ResponseEntity<byte[]> getMemberPhoto(@PathVariable Long userId) {
        PhotoPayload photo = profileService.getMemberPhotoForCoach(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.data());
    }

    @GetMapping("/metrics")
    public CoachMetricsResponse getMetrics() {
        return profileService.getCoachMetrics();
    }

    @GetMapping("/reports/age-groups")
    public AgeGroupReportResponse ageGroupReport() {
        return profileService.getAgeGroupReport();
    }
}
