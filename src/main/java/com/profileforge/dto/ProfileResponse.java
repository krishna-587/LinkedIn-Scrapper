package com.profileforge.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileResponse {
    private String firstName;
    private String lastName;
    private String headline;
    private String location;
    private String about;
    private String profilePictureUrl;
    private List<String> skills = new ArrayList<>();
    private List<Experience> experience = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private List<Certification> certifications = new ArrayList<>();
}
