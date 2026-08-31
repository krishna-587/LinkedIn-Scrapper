package com.profileforge.controller;

import com.profileforge.dto.ProfileRequest;
import com.profileforge.dto.ProfileResponse;
import com.profileforge.service.LinkedInProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final LinkedInProfileService linkedInProfileService;

    public ProfileController(LinkedInProfileService linkedInProfileService) {
        this.linkedInProfileService = linkedInProfileService;
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse profile = linkedInProfileService.fetchProfile(request.getUrl());
        return ResponseEntity.status(HttpStatus.OK).body(profile);
    }
}
