# workmyresume
Resume Analyzer App

# Things it should do (MVP)

[] User uploads Resume
[] Store the resume
[] Analyze the resume (LLM)
[] Results with a score,suggestion,strength


# Data Model    

User:
id
email

Resume:
id
user_id
raw_text
created_at

Analysis:
id
resume_id
job_description
score
strengths(JSON)
gaps(JSON)
suggestions(JSON)


✔ Foundation
✔ Signup
✔ Login
🔄 JWT
⬜ Authorization
⬜ Billing (plans)
⬜ Enforcement
⬜ Payments
⬜ Resume features
⬜ OAuth
⬜ Testing
⬜ Production hardening
