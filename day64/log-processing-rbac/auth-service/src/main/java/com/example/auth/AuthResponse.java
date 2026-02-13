package com.example.auth;

import java.util.Set;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private Set<String> roles;
    private Set<String> teams;

    public AuthResponse(String accessToken, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = user.getUsername();
        this.roles = user.getRoles();
        this.teams = user.getTeams();
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getUsername() { return username; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getTeams() { return teams; }
}
