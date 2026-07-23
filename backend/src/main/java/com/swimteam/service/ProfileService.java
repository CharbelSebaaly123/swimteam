package com.swimteam.service;

import com.swimteam.domain.MemberProfile;
import com.swimteam.domain.Role;
import com.swimteam.domain.User;
import com.swimteam.dto.AgeGroupReportResponse;
import com.swimteam.dto.AgeGroupReportResponse.AgeGroupBucket;
import com.swimteam.dto.CoachMetricsResponse;
import com.swimteam.dto.MemberSummaryResponse;
import com.swimteam.dto.ProfileResponse;
import com.swimteam.dto.ProfileUpdateRequest;
import com.swimteam.repository.MemberProfileRepository;
import com.swimteam.repository.UserRepository;
import com.swimteam.security.UserPrincipal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
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

        String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        user.setEmail(newEmail);

        applyUpdate(profile, request);
        profile.recomputeCompletion();
        memberProfileRepository.save(profile);
        userRepository.save(user);
        return ProfileResponse.from(user, profile);
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> listMembersForCoach(String sort, String direction) {
        List<MemberSummaryResponse> members = memberProfileRepository.findAllMemberProfiles().stream()
                .map(this::toSummary)
                .toList();

        String sortKey = sort == null ? "name" : sort.trim().toLowerCase(Locale.ROOT);
        boolean ascending = direction == null || !"desc".equalsIgnoreCase(direction.trim());

        Comparator<MemberSummaryResponse> comparator = switch (sortKey) {
            case "age" -> Comparator.comparing(
                    MemberSummaryResponse::getAge, Comparator.nullsLast(Integer::compareTo));
            case "name" -> Comparator.comparing(
                    (MemberSummaryResponse m) ->
                            (nullToEmpty(m.getLastName()) + " " + nullToEmpty(m.getFirstName()))
                                    .toLowerCase(Locale.ROOT));
            case "completed" -> Comparator.comparing(MemberSummaryResponse::isProfileCompleted);
            default -> Comparator.comparing(MemberSummaryResponse::getUsername, String.CASE_INSENSITIVE_ORDER);
        };

        return members.stream().sorted(ascending ? comparator : comparator.reversed()).toList();
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

    /**
     * Groups members into common swim age brackets for coach reporting.
     */
    @Transactional(readOnly = true)
    public AgeGroupReportResponse getAgeGroupReport() {
        List<AgeGroupBucket> buckets = List.of(
                new AgeGroupBucket("8 & under", 0, 8),
                new AgeGroupBucket("9–10", 9, 10),
                new AgeGroupBucket("11–12", 11, 12),
                new AgeGroupBucket("13–14", 13, 14),
                new AgeGroupBucket("15–16", 15, 16),
                new AgeGroupBucket("17–18", 17, 18),
                new AgeGroupBucket("19+", 19, 120));

        // Use mutable copies so we can add members
        List<AgeGroupBucket> groups = new ArrayList<>();
        for (AgeGroupBucket template : buckets) {
            groups.add(new AgeGroupBucket(template.getLabel(), template.getAgeFrom(), template.getAgeTo()));
        }

        long unknown = 0;
        for (MemberProfile profile : memberProfileRepository.findAllMemberProfiles()) {
            MemberSummaryResponse summary = toSummary(profile);
            Integer age = summary.getAge();
            if (age == null) {
                unknown++;
                continue;
            }
            for (AgeGroupBucket group : groups) {
                if (age >= group.getAgeFrom() && age <= group.getAgeTo()) {
                    group.addMember(summary);
                    break;
                }
            }
        }

        return new AgeGroupReportResponse(groups, unknown);
    }

    private MemberSummaryResponse toSummary(MemberProfile profile) {
        User user = profile.getUser();
        Integer age = ageFrom(profile.getDateOfBirth());
        return new MemberSummaryResponse(
                user.getId(),
                profile.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : null,
                age,
                profile.getStrokeSpecialty(),
                profile.getPersonalBestSeconds(),
                profile.isProfileCompleted());
    }

    private static Integer ageFrom(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
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
        profile.setPhone(request.getPhone().trim());
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
