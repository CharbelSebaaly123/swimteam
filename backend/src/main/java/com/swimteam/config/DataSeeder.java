package com.swimteam.config;

import com.swimteam.domain.MemberProfile;
import com.swimteam.domain.Role;
import com.swimteam.domain.User;
import com.swimteam.repository.UserRepository;
import com.swimteam.service.ImageProcessingService;
import com.swimteam.service.ImageProcessingService.ProcessedImage;
import java.io.InputStream;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SAMPLE_MEMBER_USERNAME = "sample";

    @Bean
    CommandLineRunner seedUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ImageProcessingService imageProcessingService,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.email}") String adminEmail) {
        return args -> {
            seedAdmin(userRepository, passwordEncoder, adminUsername, adminPassword, adminEmail);
            seedSampleMember(userRepository, passwordEncoder, imageProcessingService);
        };
    }

    private void seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String adminUsername,
            String adminPassword,
            String adminEmail) {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.COACH);
            userRepository.save(admin);
            log.info("Default coach user '{}' created", adminUsername);
        } else {
            log.info("Default coach user '{}' already exists", adminUsername);
        }
    }

    private void seedSampleMember(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ImageProcessingService imageProcessingService) {
        if (userRepository.existsByUsername(SAMPLE_MEMBER_USERNAME)) {
            log.info("Sample member '{}' already exists", SAMPLE_MEMBER_USERNAME);
            return;
        }

        User member = new User();
        member.setUsername(SAMPLE_MEMBER_USERNAME);
        member.setEmail("maroun.waked@swimteam.local");
        member.setPasswordHash(passwordEncoder.encode("sample123"));
        member.setRole(Role.MEMBER);

        MemberProfile profile = new MemberProfile();
        profile.setFirstName("Maroun Labib");
        profile.setLastName("Waked");
        profile.setPhone("+14155550123");
        profile.setDateOfBirth(LocalDate.of(1996, 3, 14));
        profile.setAddress("42 Harbor Lane");
        profile.setEmergencyContactName("Family Contact");
        profile.setEmergencyContactPhone("+14155550987");
        profile.setStrokeSpecialty("Freestyle");
        profile.setPersonalBestSeconds(54.8);
        profile.setHeightCm(180);
        profile.setWeightKg(75.0);
        profile.setNotes("Sample member used for demos.");
        attachSamplePhoto(profile, imageProcessingService);
        profile.recomputeCompletion();

        member.setProfile(profile);
        userRepository.save(member);
        log.info(
                "Sample member '{}' created (password: sample123) with profile photo",
                SAMPLE_MEMBER_USERNAME);
    }

    private void attachSamplePhoto(MemberProfile profile, ImageProcessingService imageProcessingService) {
        ClassPathResource photo = new ClassPathResource("sample-data/sample-member.jpg");
        if (!photo.exists()) {
            log.warn("Sample photo resource not found; sample member will have no photo");
            return;
        }
        try (InputStream in = photo.getInputStream()) {
            ProcessedImage processed = imageProcessingService.processImageStream(in);
            profile.setPhotoData(processed.data());
            profile.setPhotoContentType(processed.contentType());
            profile.setPhotoUploaded(true);
            log.info(
                    "Sample member photo compressed to {} bytes ({})",
                    processed.data().length,
                    processed.contentType());
        } catch (Exception ex) {
            log.warn("Could not attach sample member photo: {}", ex.getMessage());
        }
    }
}
