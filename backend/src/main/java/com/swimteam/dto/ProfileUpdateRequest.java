package com.swimteam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 40)
    private String phone;

    private LocalDate dateOfBirth;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 40)
    private String emergencyContactPhone;

    @Size(max = 80)
    private String strokeSpecialty;

    @Positive
    private Double personalBestSeconds;

    @Positive
    private Integer heightCm;

    @Positive
    private Double weightKg;

    @Size(max = 1000)
    private String notes;

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
}
