package com.swimteam.web;

import com.swimteam.dto.AgeGroupReportResponse;
import com.swimteam.dto.CoachMetricsResponse;
import com.swimteam.dto.MemberSummaryResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.service.ProfileService;
import com.swimteam.service.ProfileService.PhotoPayload;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final ProfileService profileService;

    public CoachController(ProfileService profileService) {
        this.profileService = profileService;
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
