package models

import (
	"regexp"
	"strings"
	"time"
)

type Role string

const (
	RoleCoach  Role = "COACH"
	RoleMember Role = "MEMBER"
)

var E164 = regexp.MustCompile(`^\+[1-9]\d{6,14}$`)

type User struct {
	ID               uint64 `gorm:"primaryKey"`
	Username         string `gorm:"size:64;uniqueIndex;not null"`
	PasswordHash     string `gorm:"not null"`
	Email            string `gorm:"uniqueIndex;not null"`
	Role             Role   `gorm:"size:16;not null"`
	FirstName        string `gorm:"size:100;default:''"`
	LastName         string `gorm:"size:100;default:''"`
	Nickname         string `gorm:"size:80"`
	PhotoData        []byte `gorm:"type:blob"`
	PhotoContentType string `gorm:"size:64"`
	PhotoUploaded    bool   `gorm:"default:false"`
	CreatedAt        time.Time
	Profile          *MemberProfile `gorm:"constraint:OnDelete:CASCADE"`
}

type MemberProfile struct {
	ID                    uint64 `gorm:"primaryKey"`
	UserID                uint64 `gorm:"uniqueIndex;not null"`
	User                  User   `gorm:"constraint:OnDelete:CASCADE"`
	FirstName             string `gorm:"size:100;default:''"`
	LastName              string `gorm:"size:100;default:''"`
	Nickname              string `gorm:"size:80"`
	Phone                 string `gorm:"size:32"`
	DateOfBirth           *time.Time
	Address               string  `gorm:"size:255"`
	EmergencyContactName  string  `gorm:"size:100"`
	EmergencyContactPhone string  `gorm:"size:32"`
	StrokeSpecialty       string  `gorm:"size:80"`
	PersonalBestSeconds   *float64
	HeightCm              *int
	WeightKg              *float64
	Notes                 string `gorm:"size:1000"`
	PhotoData             []byte `gorm:"type:blob"`
	PhotoContentType      string `gorm:"size:64"`
	PhotoUploaded         bool   `gorm:"default:false"`
	ProfileCompleted      bool   `gorm:"default:false"`
	UpdatedAt             time.Time
}

func (p *MemberProfile) AgeYears(now time.Time) *int {
	if p.DateOfBirth == nil {
		return nil
	}
	dob := *p.DateOfBirth
	age := now.Year() - dob.Year()
	if now.YearDay() < dob.YearDay() {
		age--
	}
	if age < 0 {
		age = 0
	}
	return &age
}

func (p *MemberProfile) RecomputeCompletion() {
	p.ProfileCompleted =
		strings.TrimSpace(p.FirstName) != "" &&
			strings.TrimSpace(p.LastName) != "" &&
			E164.MatchString(strings.TrimSpace(p.Phone)) &&
			p.DateOfBirth != nil &&
			strings.TrimSpace(p.EmergencyContactName) != "" &&
			E164.MatchString(strings.TrimSpace(p.EmergencyContactPhone)) &&
			strings.TrimSpace(p.StrokeSpecialty) != ""
}

func IsValidE164(phone string) bool {
	return E164.MatchString(strings.TrimSpace(phone))
}
