package handlers

import (
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/middleware"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/services"
	"github.com/go-chi/chi/v5"
)

type API struct {
	Svc *services.Service
}

func (a *API) Signup(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Username string `json:"username"`
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := decodeJSON(r, &req); err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	user, profile, token, err := a.Svc.Signup(req.Username, req.Email, req.Password)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, toAuthResponse(token, user, profile))
}

func (a *API) Login(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := decodeJSON(r, &req); err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	user, profile, token, err := a.Svc.Login(req.Username, req.Password)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, toAuthResponse(token, user, profile))
}

func (a *API) ChangePassword(w http.ResponseWriter, r *http.Request) {
	claims, ok := middleware.ClaimsFrom(r.Context())
	if !ok {
		writeMessage(w, http.StatusUnauthorized, "Unauthorized")
		return
	}
	var req struct {
		CurrentPassword string `json:"currentPassword"`
		NewPassword     string `json:"newPassword"`
	}
	if err := decodeJSON(r, &req); err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	if err := a.Svc.ChangePassword(claims.UserID, req.CurrentPassword, req.NewPassword); err != nil {
		mapServiceError(w, err)
		return
	}
	writeMessage(w, http.StatusOK, "Password changed")
}

func (a *API) GetMyProfile(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	user, profile, err := a.Svc.GetMemberProfile(claims.UserID)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, toProfileResponse(user, profile))
}

func (a *API) UpdateMyProfile(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	var req ProfileUpdateRequest
	if err := decodeJSON(r, &req); err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	user, profile, err := a.Svc.UpdateMemberProfile(claims.UserID, services.ProfileInput{
		FirstName:             req.FirstName,
		LastName:              req.LastName,
		Nickname:              req.Nickname,
		Email:                 req.Email,
		Phone:                 req.Phone,
		DateOfBirth:           req.DateOfBirth,
		Address:               req.Address,
		EmergencyContactName:  req.EmergencyContactName,
		EmergencyContactPhone: req.EmergencyContactPhone,
		StrokeSpecialty:       req.StrokeSpecialty,
		PersonalBestSeconds:   req.PersonalBestSeconds,
		HeightCm:              req.HeightCm,
		WeightKg:              req.WeightKg,
		Notes:                 req.Notes,
	})
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, toProfileResponse(user, profile))
}

func (a *API) UploadMyPhoto(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	file, header, err := r.FormFile("file")
	if err != nil {
		writeMessage(w, http.StatusBadRequest, "file is required")
		return
	}
	defer file.Close()
	res, err := a.Svc.UploadMemberPhoto(claims.UserID, header.Filename, header.Header.Get("Content-Type"), file)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, PhotoUploadResponse{
		HasPhoto: true, ContentType: res.ContentType, SizeBytes: res.SizeBytes, Message: "Photo uploaded",
	})
}

func (a *API) GetMyPhoto(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	data, ct, err := a.Svc.GetMemberPhoto(claims.UserID)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	w.Header().Set("Content-Type", ct)
	w.Header().Set("Cache-Control", "private, max-age=3600")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(data)
}

func (a *API) DeleteMyPhoto(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	if err := a.Svc.DeleteMemberPhoto(claims.UserID); err != nil {
		mapServiceError(w, err)
		return
	}
	writeMessage(w, http.StatusOK, "Photo removed")
}

func (a *API) GetCoachMe(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	user, err := a.Svc.GetCoach(claims.UserID)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, CoachProfileResponse{
		UserID: user.ID, Username: user.Username, Email: user.Email, Role: user.Role,
		FirstName: user.FirstName, LastName: user.LastName, Nickname: user.Nickname, HasPhoto: user.PhotoUploaded,
	})
}

func (a *API) UpdateCoachMe(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	var req struct {
		FirstName string `json:"firstName"`
		LastName  string `json:"lastName"`
		Nickname  string `json:"nickname"`
	}
	if err := decodeJSON(r, &req); err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	user, err := a.Svc.UpdateCoach(claims.UserID, req.FirstName, req.LastName, req.Nickname)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, CoachProfileResponse{
		UserID: user.ID, Username: user.Username, Email: user.Email, Role: user.Role,
		FirstName: user.FirstName, LastName: user.LastName, Nickname: user.Nickname, HasPhoto: user.PhotoUploaded,
	})
}

func (a *API) UploadCoachPhoto(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	file, header, err := r.FormFile("file")
	if err != nil {
		writeMessage(w, http.StatusBadRequest, "file is required")
		return
	}
	defer file.Close()
	res, err := a.Svc.UploadCoachPhoto(claims.UserID, header.Filename, header.Header.Get("Content-Type"), file)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, PhotoUploadResponse{
		HasPhoto: true, ContentType: res.ContentType, SizeBytes: res.SizeBytes, Message: "Photo uploaded",
	})
}

func (a *API) GetCoachPhoto(w http.ResponseWriter, r *http.Request) {
	claims, _ := middleware.ClaimsFrom(r.Context())
	data, ct, err := a.Svc.GetCoachPhoto(claims.UserID)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	w.Header().Set("Content-Type", ct)
	w.Header().Set("Cache-Control", "private, max-age=3600")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(data)
}

func (a *API) ListMembers(w http.ResponseWriter, r *http.Request) {
	sortBy := r.URL.Query().Get("sort")
	if sortBy == "" {
		sortBy = "name"
	}
	direction := r.URL.Query().Get("direction")
	if direction == "" {
		direction = "asc"
	}
	members, err := a.Svc.ListMembers(sortBy, direction)
	if err != nil {
		mapServiceError(w, err)
		return
	}
	out := make([]MemberSummaryResponse, 0, len(members))
	for _, m := range members {
		out = append(out, toMemberSummary(&m.User, &m.Profile))
	}
	writeJSON(w, http.StatusOK, out)
}

func (a *API) GetMember(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseUint(chi.URLParam(r, "userId"), 10, 64)
	if err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid user id")
		return
	}
	user, profile, err := a.Svc.GetMemberForCoach(id)
	if err != nil {
		if errors.Is(err, services.ErrNotFound) {
			writeMessage(w, http.StatusNotFound, "Member not found")
			return
		}
		mapServiceError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, toProfileResponse(user, profile))
}

func (a *API) GetMemberPhoto(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseUint(chi.URLParam(r, "userId"), 10, 64)
	if err != nil {
		writeMessage(w, http.StatusBadRequest, "Invalid user id")
		return
	}
	data, ct, err := a.Svc.GetMemberPhoto(id)
	if err != nil {
		if errors.Is(err, services.ErrNotFound) || errors.Is(err, services.ErrForbidden) {
			writeMessage(w, http.StatusNotFound, "Member not found")
			return
		}
		mapServiceError(w, err)
		return
	}
	w.Header().Set("Content-Type", ct)
	w.Header().Set("Cache-Control", "private, max-age=3600")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(data)
}

func (a *API) Metrics(w http.ResponseWriter, r *http.Request) {
	total, completed, withPB, avgPB, err := a.Svc.Metrics()
	if err != nil {
		mapServiceError(w, err)
		return
	}
	incomplete := total - completed
	rate := 0.0
	if total > 0 {
		rate = float64(int(float64(completed)*1000/float64(total)+0.5)) / 10
	}
	writeJSON(w, http.StatusOK, CoachMetricsResponse{
		TotalMembers:               total,
		CompletedProfiles:          completed,
		IncompleteProfiles:         incomplete,
		CompletionRatePercent:      rate,
		AveragePersonalBestSeconds: avgPB,
		MembersWithPersonalBest:    withPB,
	})
}

func (a *API) AgeGroups(w http.ResponseWriter, r *http.Request) {
	members, err := a.Svc.ListMembers("age", "asc")
	if err != nil {
		mapServiceError(w, err)
		return
	}
	brackets := services.AgeBrackets()
	groups := make([]AgeGroup, len(brackets))
	for i, b := range brackets {
		groups[i] = AgeGroup{Label: b.Label, AgeFrom: b.AgeFrom, AgeTo: b.AgeTo, Members: []MemberSummaryResponse{}}
	}
	unknown := []MemberSummaryResponse{}
	for _, m := range members {
		sum := toMemberSummary(&m.User, &m.Profile)
		if sum.Age == nil {
			unknown = append(unknown, sum)
			continue
		}
		placed := false
		for i := range groups {
			if *sum.Age >= groups[i].AgeFrom && *sum.Age <= groups[i].AgeTo {
				groups[i].Members = append(groups[i].Members, sum)
				groups[i].MemberCount++
				placed = true
				break
			}
		}
		if !placed {
			unknown = append(unknown, sum)
		}
	}
	writeJSON(w, http.StatusOK, AgeGroupReportResponse{Groups: groups, MembersWithUnknownAge: unknown})
}

func mapServiceError(w http.ResponseWriter, err error) {
	msg := err.Error()
	switch {
	case errors.Is(err, services.ErrInvalidCreds):
		writeMessage(w, http.StatusUnauthorized, "Invalid username or password")
	case errors.Is(err, services.ErrWrongPassword):
		writeMessage(w, http.StatusBadRequest, "Current password is incorrect")
	case errors.Is(err, services.ErrSamePassword):
		writeMessage(w, http.StatusBadRequest, "New password must be different from the current password")
	case errors.Is(err, services.ErrForbidden):
		writeMessage(w, http.StatusForbidden, "Only team members have personal profiles")
	case errors.Is(err, services.ErrConflict):
		writeMessage(w, http.StatusConflict, trimPrefix(msg, "conflict: "))
	case errors.Is(err, services.ErrBadRequest):
		writeMessage(w, http.StatusBadRequest, trimPrefix(msg, "bad request: "))
	case errors.Is(err, services.ErrNotFound):
		if strings.Contains(strings.ToLower(msg), "photo") || msg == "not found" {
			writeMessage(w, http.StatusNotFound, "No photo uploaded")
		} else {
			writeMessage(w, http.StatusNotFound, "User not found")
		}
	default:
		writeMessage(w, http.StatusInternalServerError, "Unexpected error: "+msg)
	}
}

func trimPrefix(s, prefix string) string {
	return strings.TrimPrefix(s, prefix)
}
