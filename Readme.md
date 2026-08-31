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
  "url": "https://www.linkedin.com/in/monu-kumar-273850177/"
}
```

Response JSON:

```json
{
  "first_name": "Monu",
  "last_name": "Kumar",
  "headline": "Building Coding Blocks School of Technology | Learning Head | Senior Mentor | Java Practice Head| Coding Blocks | CBSOT ",
  "location": "Delhi, India",
  "about": "Senior Mentor and Java practice lead",
  "profile_picture_url": "https://media.licdn.com/dms/image/v2/D4E03AQFC8lDPzBHfTg/profile-displayphoto-scale_400_400/B4EZ3Qh7NZJMAk-/0/1777320049355?e=1789603200&v=beta&t=Qik44UWGN_fmP9xLTJ4uUItI0w2pyJZO-FSo9gL7M8g",
  "skills": [
    "c",
    "c++",
    "Java",
    "Data Structures",
    "Python (Programming Language)",
    "Database Management System (DBMS)",
    "Object-Oriented Programming (OOP)",
    "HTML",
    "computer network",
    "Git",
    "Cascading Style Sheets (CSS)",
    "JavaScript",
    "Teaching"
  ],
  "experience": [
    {
      "company": "Coding Blocks School of Technology",
      "title": "Building Coding Blocks School of Technology",
      "location": "Delhi, India"
    },
    {
      "company": "Coding Blocks",
      "title": "Senior Mentor and Java practice head, Coding Blocks",
      "location": "New Delhi"
    },
    {
      "company": "Coding Blocks",
      "title": "Senior Mentor and Java practice lead, Coding Blocks",
      "location": null
    },
    {
      "company": "Coding Blocks",
      "title": "Instructor and Product Engineer",
      "location": "New Delhi, Delhi, India"
    },
    {
      "company": "Chegg India ",
      "title": "Subject Matter Expert",
      "location": "New Delhi "
    }
  ],
  "education": [
    {
      "school": "Jamia Millia Islamia",
      "degree": "Bachelor of Technology - BTech",
      "field_of_study": "Computer engineering "
    }
  ],
  "certifications": [
    {
      "name": "CODATHON\n",
      "authority": "Maulana Azad National Institute of Technology",
      "license_number": null
    }
  ]
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




