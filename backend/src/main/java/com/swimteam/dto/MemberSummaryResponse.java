package com.swimteam.dto;

public class MemberSummaryResponse {

    private Long userId;
    private Long profileId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String dateOfBirth;
    private Integer age;
    private String strokeSpecialty;
    private Double personalBestSeconds;
    private boolean profileCompleted;

    public MemberSummaryResponse(
            Long userId,
            Long profileId,
            String username,
            String email,
            String firstName,
            String lastName,
            String phone,
            String dateOfBirth,
            Integer age,
            String strokeSpecialty,
            Double personalBestSeconds,
            boolean profileCompleted) {
        this.userId = userId;
        this.profileId = profileId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.strokeSpecialty = strokeSpecialty;
        this.personalBestSeconds = personalBestSeconds;
        this.profileCompleted = profileCompleted;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProfileId() {
        return profileId;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public String getStrokeSpecialty() {
        return strokeSpecialty;
    }

    public Double getPersonalBestSeconds() {
        return personalBestSeconds;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }
}
