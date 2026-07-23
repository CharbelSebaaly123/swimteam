package com.swimteam.dto;

public class CoachMetricsResponse {

    private long totalMembers;
    private long completedProfiles;
    private long incompleteProfiles;
    private double completionRatePercent;
    private double averagePersonalBestSeconds;
    private long membersWithPersonalBest;

    public CoachMetricsResponse(
            long totalMembers,
            long completedProfiles,
            long incompleteProfiles,
            double completionRatePercent,
            double averagePersonalBestSeconds,
            long membersWithPersonalBest) {
        this.totalMembers = totalMembers;
        this.completedProfiles = completedProfiles;
        this.incompleteProfiles = incompleteProfiles;
        this.completionRatePercent = completionRatePercent;
        this.averagePersonalBestSeconds = averagePersonalBestSeconds;
        this.membersWithPersonalBest = membersWithPersonalBest;
    }

    public long getTotalMembers() {
        return totalMembers;
    }

    public long getCompletedProfiles() {
        return completedProfiles;
    }

    public long getIncompleteProfiles() {
        return incompleteProfiles;
    }

    public double getCompletionRatePercent() {
        return completionRatePercent;
    }

    public double getAveragePersonalBestSeconds() {
        return averagePersonalBestSeconds;
    }

    public long getMembersWithPersonalBest() {
        return membersWithPersonalBest;
    }
}
