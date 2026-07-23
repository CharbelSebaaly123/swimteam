package db

import (
	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Open(path string) (*gorm.DB, error) {
	gdb, err := gorm.Open(sqlite.Open(path), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		return nil, err
	}
	if err := gdb.AutoMigrate(&models.User{}, &models.MemberProfile{}); err != nil {
		return nil, err
	}
	return gdb, nil
}
