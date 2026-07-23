package middleware

import "encoding/json"

func jsonEncode(w interface{ Write([]byte) (int, error) }, body any) error {
	return json.NewEncoder(w).Encode(body)
}
