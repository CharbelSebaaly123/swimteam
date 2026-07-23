package com.swimteam.dto;

import com.swimteam.domain.Role;
import com.swimteam.domain.User;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    private String nickname;
    private boolean hasPhoto;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.nickname = user.getNickname();
        this.hasPhoto = user.hasPhoto();

        // Members primarily store display names on their profile
        if (user.getProfile() != null) {
            if (isBlank(this.firstName)) {
                this.firstName = user.getProfile().getFirstName();
            }
            if (isBlank(this.lastName)) {
                this.lastName = user.getProfile().getLastName();
            }
            if (isBlank(this.nickname)) {
                this.nickname = user.getProfile().getNickname();
            }
            if (!this.hasPhoto) {
                this.hasPhoto = user.getProfile().hasPhoto();
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
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

    public Role getRole() {
        return role;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }
}
