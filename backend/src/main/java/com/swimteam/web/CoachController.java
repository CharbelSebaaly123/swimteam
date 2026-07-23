package com.swimteam.web;

import com.swimteam.dto.AgeGroupReportResponse;
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
    public List<MemberSummaryResponse> listMembers(
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return profileService.listMembersForCoach(sort, direction);
    }

    @GetMapping("/members/{userId}")
    public ProfileResponse getMember(@PathVariable Long userId) {
        return profileService.getMemberProfileForCoach(userId);
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
