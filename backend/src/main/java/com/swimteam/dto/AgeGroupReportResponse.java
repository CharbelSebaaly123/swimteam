package com.swimteam.dto;

import java.util.ArrayList;
import java.util.List;

public class AgeGroupReportResponse {

    private List<AgeGroupBucket> groups = new ArrayList<>();
    private long membersWithUnknownAge;

    public AgeGroupReportResponse() {}

    public AgeGroupReportResponse(List<AgeGroupBucket> groups, long membersWithUnknownAge) {
        this.groups = groups;
        this.membersWithUnknownAge = membersWithUnknownAge;
    }

    public List<AgeGroupBucket> getGroups() {
        return groups;
    }

    public void setGroups(List<AgeGroupBucket> groups) {
        this.groups = groups;
    }

    public long getMembersWithUnknownAge() {
        return membersWithUnknownAge;
    }

    public void setMembersWithUnknownAge(long membersWithUnknownAge) {
        this.membersWithUnknownAge = membersWithUnknownAge;
    }

    public static class AgeGroupBucket {
        private String label;
        private Integer ageFrom;
        private Integer ageTo;
        private long memberCount;
        private List<MemberSummaryResponse> members = new ArrayList<>();

        public AgeGroupBucket() {}

        public AgeGroupBucket(String label, Integer ageFrom, Integer ageTo) {
            this.label = label;
            this.ageFrom = ageFrom;
            this.ageTo = ageTo;
        }

        public String getLabel() {
            return label;
        }

        public Integer getAgeFrom() {
            return ageFrom;
        }

        public Integer getAgeTo() {
            return ageTo;
        }

        public long getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(long memberCount) {
            this.memberCount = memberCount;
        }

        public List<MemberSummaryResponse> getMembers() {
            return members;
        }

        public void addMember(MemberSummaryResponse member) {
            this.members.add(member);
            this.memberCount = this.members.size();
        }
    }
}
