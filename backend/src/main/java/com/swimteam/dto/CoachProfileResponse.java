package com.swimteam.dto;

import com.swimteam.domain.Role;
import com.swimteam.domain.User;

public class CoachProfileResponse {

    private Long userId;
    private String username;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    private String nickname;
    private boolean hasPhoto;

    public static CoachProfileResponse from(User user) {
        CoachProfileResponse response = new CoachProfileResponse();
        response.userId = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.firstName = user.getFirstName();
        response.lastName = user.getLastName();
        response.nickname = user.getNickname();
        response.hasPhoto = user.hasPhoto();
        return response;
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
