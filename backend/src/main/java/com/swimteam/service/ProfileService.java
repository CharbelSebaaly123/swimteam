package com.swimteam.service;

import com.swimteam.domain.MemberProfile;
import com.swimteam.domain.Role;
import com.swimteam.domain.User;
import com.swimteam.dto.CoachMetricsResponse;
import com.swimteam.dto.MemberSummaryResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.dto.ProfileUpdateRequest;
import com.swimteam.repository.MemberProfileRepository;
import com.swimteam.repository.UserRepository;
import com.swimteam.security.UserPrincipal;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public ProfileService(UserRepository userRepository, MemberProfileRepository memberProfileRepository) {
        this.userRepository = userRepository;
        this.memberProfileRepository = memberProfileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UserPrincipal principal) {
        User user = requireUser(principal.getId());
        ensureMember(user);
        MemberProfile profile = ensureProfile(user);
        return ProfileResponse.from(user, profile);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UserPrincipal principal, ProfileUpdateRequest request) {
        User user = requireUser(principal.getId());
        ensureMember(user);
        MemberProfile profile = ensureProfile(user);
        applyUpdate(profile, request);
        profile.recomputeCompletion();
        memberProfileRepository.save(profile);
        return ProfileResponse.from(user, profile);
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> listMembersForCoach() {
        return memberProfileRepository.findAllMemberProfiles().stream()
                .map(profile -> {
                    User user = profile.getUser();
                    return new MemberSummaryResponse(
                            user.getId(),
                            profile.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            profile.getFirstName(),
                            profile.getLastName(),
                            profile.getStrokeSpecialty(),
                            profile.getPersonalBestSeconds(),
                            profile.isProfileCompleted());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMemberProfileForCoach(Long userId) {
        User user = requireUser(userId);
        if (user.getRole() != Role.MEMBER) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
        }
        MemberProfile profile = ensureProfile(user);
        return ProfileResponse.from(user, profile);
    }

    @Transactional(readOnly = true)
    public CoachMetricsResponse getCoachMetrics() {
        List<MemberProfile> profiles = memberProfileRepository.findAllMemberProfiles();
        long total = profiles.size();
        long completed = profiles.stream().filter(MemberProfile::isProfileCompleted).count();
        long incomplete = total - completed;
        double rate = total == 0 ? 0.0 : (completed * 100.0) / total;

        DoubleSummaryStatistics pbStats = profiles.stream()
                .map(MemberProfile::getPersonalBestSeconds)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        double avgPb = pbStats.getCount() == 0 ? 0.0 : pbStats.getAverage();
        return new CoachMetricsResponse(
                total, completed, incomplete, round1(rate), round2(avgPb), pbStats.getCount());
    }

    private User requireUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureMember(User user) {
        if (user.getRole() != Role.MEMBER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only team members have personal profiles");
        }
    }

    private MemberProfile ensureProfile(User user) {
        MemberProfile profile = user.getProfile();
        if (profile == null) {
            profile = memberProfileRepository
                    .findByUserId(user.getId())
                    .orElseGet(() -> {
                        MemberProfile created = new MemberProfile();
                        user.setProfile(created);
                        return memberProfileRepository.save(created);
                    });
        }
        return profile;
    }

    private void applyUpdate(MemberProfile profile, ProfileUpdateRequest request) {
        profile.setFirstName(request.getFirstName().trim());
        profile.setLastName(request.getLastName().trim());
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAddress(trimToNull(request.getAddress()));
        profile.setEmergencyContactName(trimToNull(request.getEmergencyContactName()));
        profile.setEmergencyContactPhone(trimToNull(request.getEmergencyContactPhone()));
        profile.setStrokeSpecialty(trimToNull(request.getStrokeSpecialty()));
        profile.setPersonalBestSeconds(request.getPersonalBestSeconds());
        profile.setHeightCm(request.getHeightCm());
        profile.setWeightKg(request.getWeightKg());
        profile.setNotes(trimToNull(request.getNotes()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
