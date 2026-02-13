package com.example.gateway;

import java.util.Set;

public class UserContext {
    private String username;
    private Long userId;
    private Set<String> roles;
    private Set<String> teams;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public Set<String> getTeams() { return teams; }
    public void setTeams(Set<String> teams) { this.teams = teams; }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        if (this.roles == null) return false;
        for (String role : roles) {
            if (this.roles.contains(role)) return true;
        }
        return false;
    }

    public boolean isInTeam(String team) {
        return teams != null && teams.contains(team);
    }
}
