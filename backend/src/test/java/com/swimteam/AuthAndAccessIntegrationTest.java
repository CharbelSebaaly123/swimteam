package com.swimteam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void coachCanLoginAndAccessCoachEndpoints() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/coach/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Labib"))
                .andExpect(jsonPath("$.lastName").value("Waked"))
                .andExpect(jsonPath("$.nickname").value("Wahsh"))
                .andExpect(jsonPath("$.hasPhoto").value(true));

        mockMvc.perform(get("/api/coach/metrics").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").exists());
    }

    @Test
    void memberCannotAccessCoachEndpoints() throws Exception {
        String username = "swimmer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = signup(username, username + "@example.com", "password123");

        mockMvc.perform(get("/api/coach/members").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsNonInternationalPhoneOnProfileUpdate() throws Exception {
        String username = "swimmer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = signup(username, username + "@example.com", "password123");

        String body = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("firstName", "Sam"),
                Map.entry("lastName", "Lee"),
                Map.entry("email", username + "@example.com"),
                Map.entry("phone", "555-0100"),
                Map.entry("dateOfBirth", "2010-01-01"),
                Map.entry("emergencyContactName", "Pat"),
                Map.entry("emergencyContactPhone", "+14155559876"),
                Map.entry("strokeSpecialty", "Freestyle")));

        mockMvc.perform(put("/api/profiles/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completesProfileWithValidInternationalPhonesAndNickname() throws Exception {
        String username = "swimmer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = signup(username, username + "@example.com", "password123");

        String body = """
                {
                  "firstName": "Sam",
                  "lastName": "Lee",
                  "nickname": "Splash",
                  "email": "%s@example.com",
                  "phone": "+14155550100",
                  "dateOfBirth": "2010-05-01",
                  "address": "",
                  "emergencyContactName": "Pat Lee",
                  "emergencyContactPhone": "+14155559876",
                  "strokeSpecialty": "Backstroke",
                  "personalBestSeconds": 61.2
                }
                """.formatted(username);

        mockMvc.perform(put("/api/profiles/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(true))
                .andExpect(jsonPath("$.nickname").value("Splash"));
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String username = "swimmer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = signup(username, username + "@example.com", "password123");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong","newPassword":"newpass1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String signup(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "email", email, "password", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return readToken(result);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return readToken(result);
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
