package com.profileforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profileforge.config.LinkedInProperties;
import com.profileforge.dto.Certification;
import com.profileforge.dto.Education;
import com.profileforge.dto.Experience;
import com.profileforge.dto.ProfileResponse;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

@Service
public class LinkedInProfileService {

    private static final String LINKEDIN_CERT_PATH = "truststore/p12/linkedin.cert";
    private static final boolean USE_CUSTOM_TRUSTSTORE = true;

    private final LinkedInProperties linkedInProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public LinkedInProfileService(LinkedInProperties linkedInProperties) {
        this.linkedInProperties = linkedInProperties;
        this.objectMapper = new ObjectMapper();
        this.webClient = buildWebClient();
    }

    private WebClient buildWebClient() {
        try {
            SslContext sslContext = createSslContext();
            HttpClient httpClient = HttpClient.create()
                    .secure(ssl -> ssl.sslContext(sslContext));

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .baseUrl("https://www.linkedin.com")
                    .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("x-restli-protocol-version", "2.0.0")
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize LinkedIn SSL context", e);
        }
    }

    private SslContext createSslContext() throws Exception {
        return createSslContext(LINKEDIN_CERT_PATH, USE_CUSTOM_TRUSTSTORE, "LinkedIn");
    }

    private SslContext createSslContext(String certPath, boolean useCustomTrustStore, String serviceName) throws Exception {
        if (!useCustomTrustStore) {
            return SslContextBuilder.forClient().build();
        }

        InputStream certInputStream = getClass().getClassLoader().getResourceAsStream(certPath);
        if (certInputStream == null) {
            return SslContextBuilder.forClient().build();
        }

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            KeyStore systemKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            String javaHome = System.getProperty("java.home");
            String cacertsPath = javaHome + "/lib/security/cacerts";

            try (InputStream cacertsStream = new FileInputStream(cacertsPath)) {
                systemKeyStore.load(cacertsStream, "changeit".toCharArray());
            } catch (Exception ignored) {
                systemKeyStore.load(null, null);
            }

            int certCount = 0;
            for (Certificate cert : certificateFactory.generateCertificates(certInputStream)) {
                systemKeyStore.setCertificateEntry("custom-cert-" + (certCount++), cert);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(systemKeyStore);

            return SslContextBuilder.forClient()
                    .trustManager(trustManagerFactory)
                    .build();
        } catch (Exception e) {
            return SslContextBuilder.forClient().build();
        }
    }

    public ProfileResponse fetchProfile(String url) {
        if (url == null || !url.contains("linkedin.com/in/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid LinkedIn profile URL");
        }

        String username = url.replaceAll("/+$", "").substring(url.replaceAll("/+$", "").lastIndexOf('/') + 1);

        String cookies = "li_at=" + linkedInProperties.getLiAt() + "; JSESSIONID=" + linkedInProperties.getJsessionid();
        String csrfToken = linkedInProperties.getJsessionid() != null ? linkedInProperties.getJsessionid().replace("\"", "") : "";

        String voyagerUrl = "https://www.linkedin.com/voyager/api/identity/dash/profiles" +
                "?q=memberIdentity&memberIdentity=" + username +
                "&decorationId=com.linkedin.voyager.dash.deco.identity.profile.FullProfileWithEntities-93";

        ResponseEntity<String> response;
        try {
            response = webClient.get()
                    .uri(voyagerUrl)
                    .header("Cookie", cookies)
                    .header("csrf-token", csrfToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(), "API error. Status: " + e.getStatusCode().value() + ". Response: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (response == null || response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile data not found in response.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse LinkedIn response");
        }

        JsonNode elements = root.path("elements");
        if (elements.isMissingNode() || elements.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile data not found in response.");
        }

        JsonNode profileNode = elements.get(0);
        ProfileResponse profile = new ProfileResponse();

        profile.setFirstName(profileNode.path("firstName").asText(""));
        profile.setLastName(profileNode.path("lastName").asText(""));
        profile.setHeadline(profileNode.path("headline").asText(""));
        profile.setAbout(profileNode.path("summary").asText(""));
        profile.setLocation(profileNode.path("geoLocation").path("geo").path("defaultLocalizedName").asText(null));

        JsonNode vectorImage = profileNode.path("profilePicture").path("displayImageReference").path("vectorImage");
        String rootUrl = vectorImage.path("rootUrl").asText(null);
        JsonNode artifacts = vectorImage.path("artifacts");
        if (rootUrl != null && artifacts.isArray() && !artifacts.isEmpty()) {
            String fileSegment = artifacts.get(artifacts.size() - 1).path("fileIdentifyingUrlPathSegment").asText("");
            profile.setProfilePictureUrl(rootUrl + fileSegment);
        }

        JsonNode skillsData = profileNode.path("profileSkills").path("elements");
        List<String> skills = new ArrayList<>();
        if (skillsData.isArray()) {
            for (JsonNode skill : skillsData) {
                String name = skill.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    skills.add(name);
                }
            }
        }
        profile.setSkills(skills);

        List<Experience> experienceList = new ArrayList<>();
        JsonNode expGroups = profileNode.path("profilePositionGroups").path("elements");
        if (expGroups.isArray()) {
            for (JsonNode group : expGroups) {
                JsonNode positions = group.path("profilePositionInPositionGroup").path("elements");
                if (positions.isArray()) {
                    for (JsonNode pos : positions) {
                        experienceList.add(new Experience(
                                pos.path("companyName").asText(""),
                                pos.path("title").asText(""),
                                pos.path("locationName").asText(null)
                        ));
                    }
                }
            }
        }
        profile.setExperience(experienceList);

        List<Education> educationList = new ArrayList<>();
        JsonNode eduElements = profileNode.path("profileEducations").path("elements");
        if (eduElements.isArray()) {
            for (JsonNode edu : eduElements) {
                educationList.add(new Education(
                        edu.path("schoolName").asText(""),
                        edu.path("degreeName").asText(null),
                        edu.path("fieldOfStudy").asText(null)
                ));
            }
        }
        profile.setEducation(educationList);

        List<Certification> certificationList = new ArrayList<>();
        JsonNode certElements = profileNode.path("profileCertifications").path("elements");
        if (certElements.isArray()) {
            for (JsonNode cert : certElements) {
                certificationList.add(new Certification(
                        cert.path("name").asText(""),
                        cert.path("authority").asText(""),
                        cert.path("licenseNumber").asText(null)
                ));
            }
        }
        profile.setCertifications(certificationList);

        return profile;
    }
}
