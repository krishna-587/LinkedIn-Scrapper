from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import httpx
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = FastAPI(title="LinkedIn Profile API")

# Load LinkedIn credentials
LI_AT = os.getenv("LI_AT")
JSESSIONID = os.getenv("JSESSIONID")

if not LI_AT or not JSESSIONID:
    print("WARNING: LinkedIn session cookies are missing from .env")

# --- Pydantic Models for Input & Output ---

class ProfileRequest(BaseModel):
    url: str

class Experience(BaseModel):
    company: str
    title: str
    location: Optional[str] = None

class Education(BaseModel):
    school: str
    degree: Optional[str] = None
    field_of_study: Optional[str] = None

class Certification(BaseModel):
    name: str
    authority: str
    license_number: Optional[str] = None

class ProfileResponse(BaseModel):
    first_name: str
    last_name: str
    headline: str
    location: Optional[str] = None
    about: Optional[str] = None
    profile_picture_url: Optional[str] = None
    skills: List[str] = []
    experience: List[Experience] = []
    education: List[Education] = []
    certifications: List[Certification] = []

# --- API Routes ---

@app.post("/api/profile", response_model=ProfileResponse)
async def get_profile(request: ProfileRequest):
    # 1. Validate URL and extract username
    if "linkedin.com/in/" not in request.url:
        raise HTTPException(status_code=400, detail="Invalid LinkedIn profile URL")

    username = request.url.rstrip('/').split('/')[-1]

    # 2. Configure HTTP Session Headers
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Cookie": f"li_at={LI_AT}; JSESSIONID={JSESSIONID}",
        "csrf-token": JSESSIONID.strip('"') if JSESSIONID else "",
        "Accept": "application/json",
        "x-restli-protocol-version": "2.0.0"
    }

    voyager_url = (
        f"https://www.linkedin.com/voyager/api/identity/dash/profiles"
        f"?q=memberIdentity&memberIdentity={username}"
        f"&decorationId=com.linkedin.voyager.dash.deco.identity.profile.FullProfileWithEntities-93"
    )

    # 3. Execute request to LinkedIn Voyager API
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(voyager_url, headers=headers)
            response.raise_for_status()
            raw_data = response.json()

        except httpx.HTTPStatusError as e:
            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"API error. Status: {e.response.status_code}. Response: {e.response.text}"
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    # 4. Parse and Structure the JSON
    elements = raw_data.get("elements", [])
    if not elements:
        raise HTTPException(status_code=404, detail="Profile data not found in response.")

    profile_node = elements[0]

    # Extract base fields
    first_name = profile_node.get("firstName", "")
    last_name = profile_node.get("lastName", "")
    headline = profile_node.get("headline", "")
    about = profile_node.get("summary", "")
    location = profile_node.get("geoLocation", {}).get("geo", {}).get("defaultLocalizedName")

    # Extract Profile Picture URL (combining rootUrl and the largest artifact)
    profile_pic_url = None
    try:
        vector_image = profile_node.get("profilePicture", {}).get("displayImageReference", {}).get("vectorImage", {})
        root_url = vector_image.get("rootUrl")
        artifacts = vector_image.get("artifacts", [])
        if root_url and artifacts:
            # Usually the last artifact in the array is the highest resolution
            profile_pic_url = root_url + artifacts[-1].get("fileIdentifyingUrlPathSegment", "")
    except Exception:
        pass # Silently fail if image structure is missing or malformed

    # Extract Skills
    skills_data = profile_node.get("profileSkills", {}).get("elements", [])
    skills = [skill.get("name") for skill in skills_data if skill.get("name")]

    # Extract Experience
    experience_list = []
    exp_groups = profile_node.get("profilePositionGroups", {}).get("elements", [])
    for group in exp_groups:
        positions = group.get("profilePositionInPositionGroup", {}).get("elements", [])
        for pos in positions:
            experience_list.append(Experience(
                company=pos.get("companyName", ""),
                title=pos.get("title", ""),
                location=pos.get("locationName")
            ))

    # Extract Education
    education_list = []
    edu_elements = profile_node.get("profileEducations", {}).get("elements", [])
    for edu in edu_elements:
        education_list.append(Education(
            school=edu.get("schoolName", ""),
            degree=edu.get("degreeName"),
            field_of_study=edu.get("fieldOfStudy")
        ))

    # Extract Certifications
    certification_list = []
    cert_elements = profile_node.get("profileCertifications", {}).get("elements", [])
    for cert in cert_elements:
        certification_list.append(Certification(
            name=cert.get("name", ""),
            authority=cert.get("authority", ""),
            license_number=cert.get("licenseNumber")
        ))

    # 5. Return the clean, validated Pydantic model
    return ProfileResponse(
        first_name=first_name,
        last_name=last_name,
        headline=headline,
        location=location,
        about=about,
        profile_picture_url=profile_pic_url,
        skills=skills,
        experience=experience_list,
        education=education_list,
        certifications=certification_list
    )