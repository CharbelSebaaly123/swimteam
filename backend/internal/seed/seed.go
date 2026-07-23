package seed

import (
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/auth"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/config"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/photo"
	"gorm.io/gorm"
)

func Run(db *gorm.DB, cfg config.Config, sampleDir string) error {
	if err := ensureCoach(db, cfg, sampleDir); err != nil {
		return err
	}
	return ensureSampleMember(db, cfg, sampleDir)
}

func ensureCoach(db *gorm.DB, cfg config.Config, sampleDir string) error {
	var count int64
	db.Model(&models.User{}).Where("username = ?", cfg.AdminUsername).Count(&count)
	if count > 0 {
		return nil
	}
	hash, err := auth.HashPassword(cfg.AdminPassword)
	if err != nil {
		return err
	}
	user := &models.User{
		Username:     cfg.AdminUsername,
		PasswordHash: hash,
		Email:        cfg.AdminEmail,
		Role:         models.RoleCoach,
		FirstName:    "Labib",
		LastName:     "Waked",
		Nickname:     "Wahsh",
	}
	if data, err := loadProcessed(filepath.Join(sampleDir, "sample-coach.jpg"), cfg); err == nil {
		user.PhotoData = data.Data
		user.PhotoContentType = data.ContentType
		user.PhotoUploaded = true
	}
	return db.Create(user).Error
}

func ensureSampleMember(db *gorm.DB, cfg config.Config, sampleDir string) error {
	var count int64
	db.Model(&models.User{}).Where("username = ?", "sample").Count(&count)
	if count > 0 {
		return nil
	}
	hash, err := auth.HashPassword("sample123")
	if err != nil {
		return err
	}
	user := &models.User{
		Username:     "sample",
		PasswordHash: hash,
		Email:        "maroun.waked@swimteam.local",
		Role:         models.RoleMember,
	}
	dob := time.Date(1996, 3, 14, 0, 0, 0, 0, time.UTC)
	pb := 54.8
	h := 180
	w := 75.0
	profile := &models.MemberProfile{
		FirstName:             "Maroun Labib",
		LastName:              "Waked",
		Nickname:              "Maroun",
		Phone:                 "+14155550123",
		DateOfBirth:           &dob,
		Address:               "42 Harbor Lane",
		EmergencyContactName:  "Family Contact",
		EmergencyContactPhone: "+14155550987",
		StrokeSpecialty:       "Freestyle",
		PersonalBestSeconds:   &pb,
		HeightCm:              &h,
		WeightKg:              &w,
		Notes:                 "Sample member used for demos.",
	}
	if data, err := loadProcessed(filepath.Join(sampleDir, "sample-member.jpg"), cfg); err == nil {
		profile.PhotoData = data.Data
		profile.PhotoContentType = data.ContentType
		profile.PhotoUploaded = true
	}
	profile.RecomputeCompletion()
	return db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(user).Error; err != nil {
			return err
		}
		profile.UserID = user.ID
		return tx.Create(profile).Error
	})
}

func loadProcessed(path string, cfg config.Config) (*photo.Result, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	res, err := photo.Process(f, filepath.Base(path), "image/jpeg", cfg.PhotoMaxDim, cfg.PhotoJPEGQuality)
	if err != nil {
		return nil, fmt.Errorf("process %s: %w", path, err)
	}
	return res, nil
}
