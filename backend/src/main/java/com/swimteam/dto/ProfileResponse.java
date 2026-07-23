package com.swimteam.dto;

import com.swimteam.domain.MemberProfile;
import com.swimteam.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

public class ProfileResponse {

    private Long profileId;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Integer age;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String strokeSpecialty;
    private Double personalBestSeconds;
    private Integer heightCm;
    private Double weightKg;
    private String notes;
    private boolean profileCompleted;
    private Instant updatedAt;

    public static ProfileResponse from(User user, MemberProfile profile) {
        ProfileResponse response = new ProfileResponse();
        response.userId = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        if (profile != null) {
            response.profileId = profile.getId();
            response.firstName = profile.getFirstName();
            response.lastName = profile.getLastName();
            response.phone = profile.getPhone();
            response.dateOfBirth = profile.getDateOfBirth();
            if (profile.getDateOfBirth() != null) {
                response.age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            }
            response.address = profile.getAddress();
            response.emergencyContactName = profile.getEmergencyContactName();
            response.emergencyContactPhone = profile.getEmergencyContactPhone();
            response.strokeSpecialty = profile.getStrokeSpecialty();
            response.personalBestSeconds = profile.getPersonalBestSeconds();
            response.heightCm = profile.getHeightCm();
            response.weightKg = profile.getWeightKg();
            response.notes = profile.getNotes();
            response.profileCompleted = profile.isProfileCompleted();
            response.updatedAt = profile.getUpdatedAt();
        }
        return response;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public String getStrokeSpecialty() {
        return strokeSpecialty;
    }

    public Double getPersonalBestSeconds() {
        return personalBestSeconds;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
