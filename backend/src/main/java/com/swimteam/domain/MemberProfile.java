package com.swimteam.domain;

import com.swimteam.validation.PhoneNumbers;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "member_profiles")
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String firstName = "";

    @Column(nullable = false)
    private String lastName = "";

    private String phone;

    private LocalDate dateOfBirth;

    private String address;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String strokeSpecialty;

    /** Personal best time in seconds for preferred stroke (metric). */
    private Double personalBestSeconds;

    private Integer heightCm;

    private Double weightKg;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean profileCompleted = false;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getStrokeSpecialty() {
        return strokeSpecialty;
    }

    public void setStrokeSpecialty(String strokeSpecialty) {
        this.strokeSpecialty = strokeSpecialty;
    }

    public Double getPersonalBestSeconds() {
        return personalBestSeconds;
    }

    public void setPersonalBestSeconds(Double personalBestSeconds) {
        this.personalBestSeconds = personalBestSeconds;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Required for completion: name, E.164 phone, DOB, emergency contact (E.164), stroke specialty.
     * Address is optional. Email is validated on the User account separately.
     */
    public void recomputeCompletion() {
        this.profileCompleted =
                isPresent(firstName)
                        && isPresent(lastName)
                        && PhoneNumbers.isValidE164(phone)
                        && dateOfBirth != null
                        && isPresent(emergencyContactName)
                        && PhoneNumbers.isValidE164(emergencyContactPhone)
                        && isPresent(strokeSpecialty);
        this.updatedAt = Instant.now();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
