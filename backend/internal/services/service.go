package services

import (
	"errors"
	"fmt"
	"math"
	"sort"
	"strings"
	"time"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/auth"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/config"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/photo"
	"gorm.io/gorm"
)

var (
	ErrUnauthorized     = errors.New("unauthorized")
	ErrForbidden        = errors.New("forbidden")
	ErrNotFound         = errors.New("not found")
	ErrConflict         = errors.New("conflict")
	ErrBadRequest       = errors.New("bad request")
	ErrInvalidCreds     = errors.New("invalid credentials")
	ErrWrongPassword    = errors.New("wrong password")
	ErrSamePassword     = errors.New("same password")
)

type Service struct {
	DB     *gorm.DB
	Tokens *auth.TokenService
	Cfg    config.Config
}

func (s *Service) Signup(username, email, password string) (*models.User, *models.MemberProfile, string, error) {
	username = strings.TrimSpace(username)
	email = strings.ToLower(strings.TrimSpace(email))
	if len(username) < 3 || len(username) > 64 {
		return nil, nil, "", fmt.Errorf("%w: username must be 3-64 characters", ErrBadRequest)
	}
	if !strings.Contains(email, "@") || len(email) > 255 {
		return nil, nil, "", fmt.Errorf("%w: invalid email", ErrBadRequest)
	}
	if len(password) < 6 || len(password) > 100 {
		return nil, nil, "", fmt.Errorf("%w: password must be 6-100 characters", ErrBadRequest)
	}

	var count int64
	s.DB.Model(&models.User{}).Where("username = ?", username).Count(&count)
	if count > 0 {
		return nil, nil, "", fmt.Errorf("%w: Username already taken", ErrConflict)
	}
	s.DB.Model(&models.User{}).Where("email = ?", email).Count(&count)
	if count > 0 {
		return nil, nil, "", fmt.Errorf("%w: Email already registered", ErrConflict)
	}

	hash, err := auth.HashPassword(password)
	if err != nil {
		return nil, nil, "", err
	}
	user := &models.User{
		Username:     username,
		PasswordHash: hash,
		Email:        email,
		Role:         models.RoleMember,
	}
	profile := &models.MemberProfile{}
	err = s.DB.Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(user).Error; err != nil {
			return err
		}
		profile.UserID = user.ID
		profile.RecomputeCompletion()
		return tx.Create(profile).Error
	})
	if err != nil {
		return nil, nil, "", err
	}
	token, err := s.Tokens.Issue(user)
	if err != nil {
		return nil, nil, "", err
	}
	return user, profile, token, nil
}

func (s *Service) Login(username, password string) (*models.User, *models.MemberProfile, string, error) {
	var user models.User
	if err := s.DB.Where("username = ?", strings.TrimSpace(username)).First(&user).Error; err != nil {
		return nil, nil, "", ErrInvalidCreds
	}
	if !auth.CheckPassword(user.PasswordHash, password) {
		return nil, nil, "", ErrInvalidCreds
	}
	var profile *models.MemberProfile
	if user.Role == models.RoleMember {
		var p models.MemberProfile
		if err := s.DB.Where("user_id = ?", user.ID).First(&p).Error; err == nil {
			profile = &p
		}
	}
	token, err := s.Tokens.Issue(&user)
	if err != nil {
		return nil, nil, "", err
	}
	return &user, profile, token, nil
}

func (s *Service) ChangePassword(userID uint64, current, next string) error {
	if len(next) < 6 || len(next) > 100 {
		return fmt.Errorf("%w: New password must be 6-100 characters", ErrBadRequest)
	}
	var user models.User
	if err := s.DB.First(&user, userID).Error; err != nil {
		return ErrNotFound
	}
	if !auth.CheckPassword(user.PasswordHash, current) {
		return ErrWrongPassword
	}
	if current == next {
		return ErrSamePassword
	}
	hash, err := auth.HashPassword(next)
	if err != nil {
		return err
	}
	return s.DB.Model(&user).Update("password_hash", hash).Error
}

func (s *Service) GetMemberProfile(userID uint64) (*models.User, *models.MemberProfile, error) {
	var user models.User
	if err := s.DB.First(&user, userID).Error; err != nil {
		return nil, nil, ErrNotFound
	}
	if user.Role != models.RoleMember {
		return nil, nil, fmt.Errorf("%w: Only team members have personal profiles", ErrForbidden)
	}
	var profile models.MemberProfile
	if err := s.DB.Where("user_id = ?", userID).First(&profile).Error; err != nil {
		return nil, nil, ErrNotFound
	}
	return &user, &profile, nil
}

type ProfileInput struct {
	FirstName             string
	LastName              string
	Nickname              string
	Email                 string
	Phone                 string
	DateOfBirth           string
	Address               string
	EmergencyContactName  string
	EmergencyContactPhone string
	StrokeSpecialty       string
	PersonalBestSeconds   *float64
	HeightCm              *int
	WeightKg              *float64
	Notes                 string
}

func (s *Service) UpdateMemberProfile(userID uint64, in ProfileInput) (*models.User, *models.MemberProfile, error) {
	user, profile, err := s.GetMemberProfile(userID)
	if err != nil {
		return nil, nil, err
	}
	if err := validateProfileInput(in); err != nil {
		return nil, nil, err
	}
	email := strings.ToLower(strings.TrimSpace(in.Email))
	var count int64
	s.DB.Model(&models.User{}).Where("email = ? AND id <> ?", email, userID).Count(&count)
	if count > 0 {
		return nil, nil, fmt.Errorf("%w: Email already registered", ErrConflict)
	}
	dob, err := time.Parse("2006-01-02", strings.TrimSpace(in.DateOfBirth))
	if err != nil {
		return nil, nil, fmt.Errorf("%w: dateOfBirth must be YYYY-MM-DD", ErrBadRequest)
	}

	profile.FirstName = strings.TrimSpace(in.FirstName)
	profile.LastName = strings.TrimSpace(in.LastName)
	profile.Nickname = strings.TrimSpace(in.Nickname)
	profile.Phone = strings.TrimSpace(in.Phone)
	profile.DateOfBirth = &dob
	profile.Address = strings.TrimSpace(in.Address)
	profile.EmergencyContactName = strings.TrimSpace(in.EmergencyContactName)
	profile.EmergencyContactPhone = strings.TrimSpace(in.EmergencyContactPhone)
	profile.StrokeSpecialty = strings.TrimSpace(in.StrokeSpecialty)
	profile.PersonalBestSeconds = in.PersonalBestSeconds
	profile.HeightCm = in.HeightCm
	profile.WeightKg = in.WeightKg
	profile.Notes = strings.TrimSpace(in.Notes)
	profile.RecomputeCompletion()
	profile.UpdatedAt = time.Now().UTC()

	err = s.DB.Transaction(func(tx *gorm.DB) error {
		if err := tx.Model(user).Update("email", email).Error; err != nil {
			return err
		}
		user.Email = email
		return tx.Save(profile).Error
	})
	if err != nil {
		return nil, nil, err
	}
	return user, profile, nil
}

func validateProfileInput(in ProfileInput) error {
	if strings.TrimSpace(in.FirstName) == "" || len(in.FirstName) > 100 {
		return fmt.Errorf("%w: firstName is required (max 100)", ErrBadRequest)
	}
	if strings.TrimSpace(in.LastName) == "" || len(in.LastName) > 100 {
		return fmt.Errorf("%w: lastName is required (max 100)", ErrBadRequest)
	}
	if len(in.Nickname) > 80 {
		return fmt.Errorf("%w: nickname max 80", ErrBadRequest)
	}
	email := strings.TrimSpace(in.Email)
	if email == "" || !strings.Contains(email, "@") || len(email) > 255 {
		return fmt.Errorf("%w: email is required", ErrBadRequest)
	}
	if !models.IsValidE164(in.Phone) {
		return fmt.Errorf("%w: phone must be international E.164 (e.g. +14155552671)", ErrBadRequest)
	}
	if strings.TrimSpace(in.DateOfBirth) == "" {
		return fmt.Errorf("%w: dateOfBirth is mandatory", ErrBadRequest)
	}
	if len(in.Address) > 255 {
		return fmt.Errorf("%w: address max 255", ErrBadRequest)
	}
	phone := strings.TrimSpace(in.EmergencyContactPhone)
	if phone != "" && !models.IsValidE164(phone) {
		return fmt.Errorf("%w: emergencyContactPhone must be E.164", ErrBadRequest)
	}
	if in.PersonalBestSeconds != nil && *in.PersonalBestSeconds <= 0 {
		return fmt.Errorf("%w: personalBestSeconds must be positive", ErrBadRequest)
	}
	if in.HeightCm != nil && *in.HeightCm <= 0 {
		return fmt.Errorf("%w: heightCm must be positive", ErrBadRequest)
	}
	if in.WeightKg != nil && *in.WeightKg <= 0 {
		return fmt.Errorf("%w: weightKg must be positive", ErrBadRequest)
	}
	if len(in.Notes) > 1000 {
		return fmt.Errorf("%w: notes max 1000", ErrBadRequest)
	}
	return nil
}

func (s *Service) UploadMemberPhoto(userID uint64, filename, contentType string, reader interface{ Read([]byte) (int, error) }) (*photo.Result, error) {
	_, profile, err := s.GetMemberProfile(userID)
	if err != nil {
		return nil, err
	}
	res, err := photo.Process(reader, filename, contentType, s.Cfg.PhotoMaxDim, s.Cfg.PhotoJPEGQuality)
	if err != nil {
		return nil, fmt.Errorf("%w: %s", ErrBadRequest, err.Error())
	}
	profile.PhotoData = res.Data
	profile.PhotoContentType = res.ContentType
	profile.PhotoUploaded = true
	profile.UpdatedAt = time.Now().UTC()
	if err := s.DB.Save(profile).Error; err != nil {
		return nil, err
	}
	return res, nil
}

func (s *Service) GetMemberPhoto(userID uint64) ([]byte, string, error) {
	_, profile, err := s.GetMemberProfile(userID)
	if err != nil {
		return nil, "", err
	}
	if !profile.PhotoUploaded || len(profile.PhotoData) == 0 {
		return nil, "", ErrNotFound
	}
	ct := profile.PhotoContentType
	if ct == "" {
		ct = "image/jpeg"
	}
	return profile.PhotoData, ct, nil
}

func (s *Service) DeleteMemberPhoto(userID uint64) error {
	_, profile, err := s.GetMemberProfile(userID)
	if err != nil {
		return err
	}
	profile.PhotoData = nil
	profile.PhotoContentType = ""
	profile.PhotoUploaded = false
	profile.UpdatedAt = time.Now().UTC()
	return s.DB.Save(profile).Error
}

func (s *Service) GetCoach(userID uint64) (*models.User, error) {
	var user models.User
	if err := s.DB.First(&user, userID).Error; err != nil {
		return nil, ErrNotFound
	}
	if user.Role != models.RoleCoach {
		return nil, ErrForbidden
	}
	return &user, nil
}

func (s *Service) UpdateCoach(userID uint64, first, last, nick string) (*models.User, error) {
	user, err := s.GetCoach(userID)
	if err != nil {
		return nil, err
	}
	first = strings.TrimSpace(first)
	last = strings.TrimSpace(last)
	nick = strings.TrimSpace(nick)
	if first == "" || last == "" {
		return nil, fmt.Errorf("%w: firstName and lastName are required", ErrBadRequest)
	}
	if len(first) > 100 || len(last) > 100 || len(nick) > 80 {
		return nil, fmt.Errorf("%w: name fields too long", ErrBadRequest)
	}
	user.FirstName = first
	user.LastName = last
	user.Nickname = nick
	if err := s.DB.Save(user).Error; err != nil {
		return nil, err
	}
	return user, nil
}

func (s *Service) UploadCoachPhoto(userID uint64, filename, contentType string, reader interface{ Read([]byte) (int, error) }) (*photo.Result, error) {
	user, err := s.GetCoach(userID)
	if err != nil {
		return nil, err
	}
	res, err := photo.Process(reader, filename, contentType, s.Cfg.PhotoMaxDim, s.Cfg.PhotoJPEGQuality)
	if err != nil {
		return nil, fmt.Errorf("%w: %s", ErrBadRequest, err.Error())
	}
	user.PhotoData = res.Data
	user.PhotoContentType = res.ContentType
	user.PhotoUploaded = true
	if err := s.DB.Save(user).Error; err != nil {
		return nil, err
	}
	return res, nil
}

func (s *Service) GetCoachPhoto(userID uint64) ([]byte, string, error) {
	user, err := s.GetCoach(userID)
	if err != nil {
		return nil, "", err
	}
	if !user.PhotoUploaded || len(user.PhotoData) == 0 {
		return nil, "", ErrNotFound
	}
	ct := user.PhotoContentType
	if ct == "" {
		ct = "image/jpeg"
	}
	return user.PhotoData, ct, nil
}

func (s *Service) ListMembers(sortBy, direction string) ([]struct {
	User    models.User
	Profile models.MemberProfile
}, error) {
	var profiles []models.MemberProfile
	if err := s.DB.Preload("User").Find(&profiles).Error; err != nil {
		return nil, err
	}
	type pair struct {
		User    models.User
		Profile models.MemberProfile
	}
	out := make([]pair, 0, len(profiles))
	for _, p := range profiles {
		if p.User.Role != models.RoleMember {
			continue
		}
		out = append(out, pair{User: p.User, Profile: p})
	}

	less := func(i, j int) bool {
		a, b := out[i], out[j]
		switch strings.ToLower(sortBy) {
		case "age":
			ai, bi := a.Profile.AgeYears(time.Now()), b.Profile.AgeYears(time.Now())
			if ai == nil && bi == nil {
				return strings.ToLower(a.User.Username) < strings.ToLower(b.User.Username)
			}
			if ai == nil {
				return false
			}
			if bi == nil {
				return true
			}
			if *ai == *bi {
				return strings.ToLower(a.User.Username) < strings.ToLower(b.User.Username)
			}
			return *ai < *bi
		case "completed":
			if a.Profile.ProfileCompleted == b.Profile.ProfileCompleted {
				return strings.ToLower(a.User.Username) < strings.ToLower(b.User.Username)
			}
			return !a.Profile.ProfileCompleted && b.Profile.ProfileCompleted
		case "username":
			return strings.ToLower(a.User.Username) < strings.ToLower(b.User.Username)
		default: // name
			an := strings.ToLower(a.Profile.LastName + " " + a.Profile.FirstName)
			bn := strings.ToLower(b.Profile.LastName + " " + b.Profile.FirstName)
			if an == bn {
				return strings.ToLower(a.User.Username) < strings.ToLower(b.User.Username)
			}
			return an < bn
		}
	}
	sort.Slice(out, less)
	if strings.EqualFold(direction, "desc") {
		for i, j := 0, len(out)-1; i < j; i, j = i+1, j-1 {
			out[i], out[j] = out[j], out[i]
		}
	}

	result := make([]struct {
		User    models.User
		Profile models.MemberProfile
	}, len(out))
	for i, p := range out {
		result[i].User = p.User
		result[i].Profile = p.Profile
	}
	return result, nil
}

func (s *Service) GetMemberForCoach(userID uint64) (*models.User, *models.MemberProfile, error) {
	user, profile, err := s.GetMemberProfile(userID)
	if err != nil {
		if errors.Is(err, ErrForbidden) {
			return nil, nil, ErrNotFound
		}
		return nil, nil, err
	}
	return user, profile, nil
}

func (s *Service) Metrics() (total, completed, withPB int, avgPB float64, err error) {
	members, err := s.ListMembers("name", "asc")
	if err != nil {
		return 0, 0, 0, 0, err
	}
	total = len(members)
	var sum float64
	for _, m := range members {
		if m.Profile.ProfileCompleted {
			completed++
		}
		if m.Profile.PersonalBestSeconds != nil {
			withPB++
			sum += *m.Profile.PersonalBestSeconds
		}
	}
	if withPB > 0 {
		avgPB = math.Round((sum/float64(withPB))*100) / 100
	}
	return total, completed, withPB, avgPB, nil
}

type AgeBracket struct {
	Label   string
	AgeFrom int
	AgeTo   int
}

func AgeBrackets() []AgeBracket {
	return []AgeBracket{
		{Label: "8 & under", AgeFrom: 0, AgeTo: 8},
		{Label: "9–10", AgeFrom: 9, AgeTo: 10},
		{Label: "11–12", AgeFrom: 11, AgeTo: 12},
		{Label: "13–14", AgeFrom: 13, AgeTo: 14},
		{Label: "15–16", AgeFrom: 15, AgeTo: 16},
		{Label: "17–18", AgeFrom: 17, AgeTo: 18},
		{Label: "19+", AgeFrom: 19, AgeTo: 120},
	}
}
