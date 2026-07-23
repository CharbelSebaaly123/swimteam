package com.swimteam.web;

import com.swimteam.dto.CoachMetricsResponse;
import com.swimteam.dto.MemberSummaryResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.service.ProfileService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final ProfileService profileService;

    public CoachController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/members")
    public List<MemberSummaryResponse> listMembers() {
        return profileService.listMembersForCoach();
    }

    @GetMapping("/members/{userId}")
    public ProfileResponse getMember(@PathVariable Long userId) {
        return profileService.getMemberProfileForCoach(userId);
    }

    @GetMapping("/metrics")
    public CoachMetricsResponse getMetrics() {
        return profileService.getCoachMetrics();
    }
}
