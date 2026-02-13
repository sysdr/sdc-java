package com.example.gateway;

public class LogQueryRequest {
    private String query;
    private String team;
    private String startTime;
    private String endTime;
    private int maxResults = 100;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
