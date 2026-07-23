package handlers

import (
	"strings"
	"time"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
)

type AuthResponse struct {
	Token     string      `json:"token"`
	TokenType string      `json:"tokenType"`
	UserID    uint64      `json:"userId"`
	Username  string      `json:"username"`
	Email     string      `json:"email"`
	Role      models.Role `json:"role"`
	FirstName string      `json:"firstName"`
	LastName  string      `json:"lastName"`
	Nickname  string      `json:"nickname"`
	HasPhoto  bool        `json:"hasPhoto"`
}

type ProfileResponse struct {
	ProfileID             uint64   `json:"profileId"`
	UserID                uint64   `json:"userId"`
	Username              string   `json:"username"`
	Email                 string   `json:"email"`
	FirstName             string   `json:"firstName"`
	LastName              string   `json:"lastName"`
	Nickname              string   `json:"nickname"`
	Phone                 string   `json:"phone"`
	DateOfBirth           *string  `json:"dateOfBirth"`
	Age                   *int     `json:"age"`
	Address               string   `json:"address"`
	EmergencyContactName  string   `json:"emergencyContactName"`
	EmergencyContactPhone string   `json:"emergencyContactPhone"`
	StrokeSpecialty       string   `json:"strokeSpecialty"`
	PersonalBestSeconds   *float64 `json:"personalBestSeconds"`
	HeightCm              *int     `json:"heightCm"`
	WeightKg              *float64 `json:"weightKg"`
	Notes                 string   `json:"notes"`
	ProfileCompleted      bool     `json:"profileCompleted"`
	HasPhoto              bool     `json:"hasPhoto"`
	UpdatedAt             time.Time `json:"updatedAt"`
}

type ProfileUpdateRequest struct {
	FirstName             string   `json:"firstName"`
	LastName              string   `json:"lastName"`
	Nickname              string   `json:"nickname"`
	Email                 string   `json:"email"`
	Phone                 string   `json:"phone"`
	DateOfBirth           string   `json:"dateOfBirth"`
	Address               string   `json:"address"`
	EmergencyContactName  string   `json:"emergencyContactName"`
	EmergencyContactPhone string   `json:"emergencyContactPhone"`
	StrokeSpecialty       string   `json:"strokeSpecialty"`
	PersonalBestSeconds   *float64 `json:"personalBestSeconds"`
	HeightCm              *int     `json:"heightCm"`
	WeightKg              *float64 `json:"weightKg"`
	Notes                 string   `json:"notes"`
}

type PhotoUploadResponse struct {
	HasPhoto    bool   `json:"hasPhoto"`
	ContentType string `json:"contentType"`
	SizeBytes   int    `json:"sizeBytes"`
	Message     string `json:"message"`
}

type CoachProfileResponse struct {
	UserID    uint64      `json:"userId"`
	Username  string      `json:"username"`
	Email     string      `json:"email"`
	Role      models.Role `json:"role"`
	FirstName string      `json:"firstName"`
	LastName  string      `json:"lastName"`
	Nickname  string      `json:"nickname"`
	HasPhoto  bool        `json:"hasPhoto"`
}

type MemberSummaryResponse struct {
	UserID              uint64   `json:"userId"`
	ProfileID           uint64   `json:"profileId"`
	Username            string   `json:"username"`
	Email               string   `json:"email"`
	FirstName           string   `json:"firstName"`
	LastName            string   `json:"lastName"`
	Nickname            string   `json:"nickname"`
	Phone               string   `json:"phone"`
	DateOfBirth         *string  `json:"dateOfBirth"`
	Age                 *int     `json:"age"`
	StrokeSpecialty     string   `json:"strokeSpecialty"`
	PersonalBestSeconds *float64 `json:"personalBestSeconds"`
	ProfileCompleted    bool     `json:"profileCompleted"`
	HasPhoto            bool     `json:"hasPhoto"`
}

type CoachMetricsResponse struct {
	TotalMembers               int     `json:"totalMembers"`
	CompletedProfiles          int     `json:"completedProfiles"`
	IncompleteProfiles         int     `json:"incompleteProfiles"`
	CompletionRatePercent      float64 `json:"completionRatePercent"`
	AveragePersonalBestSeconds float64 `json:"averagePersonalBestSeconds"`
	MembersWithPersonalBest    int     `json:"membersWithPersonalBest"`
}

type AgeGroup struct {
	Label       string                  `json:"label"`
	AgeFrom     int                     `json:"ageFrom"`
	AgeTo       int                     `json:"ageTo"`
	MemberCount int                     `json:"memberCount"`
	Members     []MemberSummaryResponse `json:"members"`
}

type AgeGroupReportResponse struct {
	Groups                []AgeGroup              `json:"groups"`
	MembersWithUnknownAge []MemberSummaryResponse `json:"membersWithUnknownAge"`
}

func toAuthResponse(token string, user *models.User, profile *models.MemberProfile) AuthResponse {
	first, last, nick := user.FirstName, user.LastName, user.Nickname
	hasPhoto := user.PhotoUploaded
	if user.Role == models.RoleMember && profile != nil {
		if strings.TrimSpace(first) == "" {
			first = profile.FirstName
		}
		if strings.TrimSpace(last) == "" {
			last = profile.LastName
		}
		if strings.TrimSpace(nick) == "" {
			nick = profile.Nickname
		}
		hasPhoto = profile.PhotoUploaded
	}
	return AuthResponse{
		Token:     token,
		TokenType: "Bearer",
		UserID:    user.ID,
		Username:  user.Username,
		Email:     user.Email,
		Role:      user.Role,
		FirstName: first,
		LastName:  last,
		Nickname:  nick,
		HasPhoto:  hasPhoto,
	}
}

func toProfileResponse(user *models.User, p *models.MemberProfile) ProfileResponse {
	var dob *string
	if p.DateOfBirth != nil {
		s := p.DateOfBirth.Format("2006-01-02")
		dob = &s
	}
	return ProfileResponse{
		ProfileID:             p.ID,
		UserID:                user.ID,
		Username:              user.Username,
		Email:                 user.Email,
		FirstName:             p.FirstName,
		LastName:              p.LastName,
		Nickname:              p.Nickname,
		Phone:                 p.Phone,
		DateOfBirth:           dob,
		Age:                   p.AgeYears(time.Now()),
		Address:               p.Address,
		EmergencyContactName:  p.EmergencyContactName,
		EmergencyContactPhone: p.EmergencyContactPhone,
		StrokeSpecialty:       p.StrokeSpecialty,
		PersonalBestSeconds:   p.PersonalBestSeconds,
		HeightCm:              p.HeightCm,
		WeightKg:              p.WeightKg,
		Notes:                 p.Notes,
		ProfileCompleted:      p.ProfileCompleted,
		HasPhoto:              p.PhotoUploaded,
		UpdatedAt:             p.UpdatedAt,
	}
}

func toMemberSummary(user *models.User, p *models.MemberProfile) MemberSummaryResponse {
	var dob *string
	if p.DateOfBirth != nil {
		s := p.DateOfBirth.Format("2006-01-02")
		dob = &s
	}
	return MemberSummaryResponse{
		UserID:              user.ID,
		ProfileID:           p.ID,
		Username:            user.Username,
		Email:               user.Email,
		FirstName:           p.FirstName,
		LastName:            p.LastName,
		Nickname:            p.Nickname,
		Phone:               p.Phone,
		DateOfBirth:         dob,
		Age:                 p.AgeYears(time.Now()),
		StrokeSpecialty:     p.StrokeSpecialty,
		PersonalBestSeconds: p.PersonalBestSeconds,
		ProfileCompleted:    p.ProfileCompleted,
		HasPhoto:            p.PhotoUploaded,
	}
}
