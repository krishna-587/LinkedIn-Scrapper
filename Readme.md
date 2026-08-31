# ProfileForge

ProfileForge is the Java Spring Boot version of the original LinkedIn profile API. It accepts a public LinkedIn profile URL and returns a flattened JSON payload with the same contract as the Python API: basic profile details, skills, experience, education, and certifications.

## Tech stack

- Java 17+
- Spring Boot 3.3
- Spring WebFlux
- Spring Validation
- Maven

## API contract

### POST /api/profile

Request JSON:

```json
{
  "url": "https://www.linkedin.com/in/aditya-chavhan-2210adi/"
}
```

Response JSON:

```json
{
  "first_name": "Aditya",
  "last_name": "Chavhan",
  "headline": "Junior Software Automation Engineer",
  "location": "Mumbai Metropolitan Region",
  "about": "Currently serving as a Junior...",
  "profile_picture_url": "https://media.licdn.com/dms/image/...",
  "skills": ["Python", "FastAPI", "Data Structures"],
  "experience": [
    {
      "company": "EPAM Systems",
      "title": "Junior Software Automation Engineer",
      "location": "Hyderabad, Telangana, India"
    }
  ],
  "education": [],
  "certifications": []
}
```

## Local setup

1. Install Java 17+ and Maven.
2. Create a .env file or export environment variables:

```bash
LI_AT=your_li_at_cookie_value
JSESSIONID="your_jsessionid_cookie_value"
```

3. Run the app:

```bash
mvn spring-boot:run
```

4. Open the API at:

- http://localhost:8080/api/profile
- Swagger UI is not enabled by default in this minimal conversion, but the endpoint is available.

## Notes

This implementation keeps the same LinkedIn Voyager scraping approach as the original app: it calls the LinkedIn internal profile endpoint using the active browser cookies and flattens the returned graph into a clean DTO structure.
```




