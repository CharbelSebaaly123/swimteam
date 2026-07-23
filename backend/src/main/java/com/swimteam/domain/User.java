package com.swimteam.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(length = 100)
    private String firstName = "";

    @Column(length = 100)
    private String lastName = "";

    /** Optional preferred nickname (especially useful for coaches). */
    @Column(length = 80)
    private String nickname;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] photoData;

    private String photoContentType;

    @Column(nullable = false)
    private boolean photoUploaded = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MemberProfile profile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public boolean isPhotoUploaded() {
        return photoUploaded;
    }

    public void setPhotoUploaded(boolean photoUploaded) {
        this.photoUploaded = photoUploaded;
    }

    public boolean hasPhoto() {
        return photoUploaded;
    }

    public String displayName() {
        String full = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public MemberProfile getProfile() {
        return profile;
    }

    public void setProfile(MemberProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }
}
