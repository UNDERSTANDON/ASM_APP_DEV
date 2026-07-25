# P5 & M4 Documentation: AI Study Mentor

This document contains the **User Guide** and **Technical Documentation** required to fulfill criteria P5 and M4, demonstrating the functionality, architecture, and deployment details of the AI Study Mentor application.

---

## Part 1: User Guide (P5)

Welcome to the **AI Study Mentor**! This guide will help you navigate the app and make the most out of your AI-powered learning journey.

### 1. Getting Started

#### Registration and Login

1. Open the application on your Android device.
2. If you are a new user, tap **Register**. Enter a valid email address and a secure password (must contain at least 8 characters, uppercase, lowercase, numbers, and special characters).
3. Existing users can simply enter their credentials and tap **Login**.
*Note: If you are under 13, you will be prompted to provide a parent/guardian's email for COPPA consent verification.*

#### Profile Setup and API Configuration

1. On your first login, complete the onboarding profile:
   - Select your **Education Level** (Middle School, High School, University).
   - Choose your preferred **Learning Subjects**.
   - Set your **Explanation Style** (Concise, Detailed, Step-by-Step).
2. To use the AI capabilities, navigate to the **Profile** tab, select **Gemini API Config**, and securely enter your Google Gemini API key.

### 2. Using the AI Study Mentor

#### Submitting a Question

You can submit questions via text or by taking a picture of your homework.

- **Text Input**: Go to the **Home** dashboard, tap the text box, type your question, select the subject category, and hit **Submit**.
- **Image Input**: Tap the **Camera Icon** next to the text box. Take a clear photo of your problem (ensure good lighting). The app will automatically extract the text using OCR. Review the extracted text and tap **Submit**.

#### Viewing AI Explanations

Within seconds, the AI will provide a structured response containing:

- **Step-by-Step Solutions**: Clear breakdown of the answer.
- **Formulas**: Rendered mathematical formulas.
- **Alternative Methods**: Different ways to solve the same problem (if applicable).
- **Common Mistakes**: Pitfalls to avoid.
You can tap the **Star Icon** to bookmark an answer for later review.

### 3. Offline Mode & History

Don't have internet access? No problem!

- Navigate to the **History** tab to view all your previously answered and bookmarked questions.
- **Offline Submissions**: If you submit a question while offline, it will be saved locally as "Saved Offline" and automatically sent to the AI once your connection is restored.

### 4. Quizzes & Progress Tracking

- **Quizzes**: Go to the **Quiz Lab** to take AI-generated practice tests based on your question history. You can filter by Grade and Semester.
- **Progress Dashboard**: Visit the **Insights Dashboard** to view your daily streak, XP points, cognitive load, and subject mastery levels. Complete actions to earn XP and level up!

---

## Part 2: Technical Documentation (M4)

This section details the underlying architecture, data models, and technical decisions that power the AI Study Mentor application.

### 1. Architecture Overview

The application follows the **Model-View-Controller (MVC)** architectural pattern combined with the **Data Access Object (DAO)** pattern. It utilizes **Clean Architecture** principles to separate the UI layer from the data and domain layers.

- **Frontend**: Native Android development using Java/Kotlin inside Android Studio, implementing Material Design 3 and Glassmorphism for UI/UX.
- **Backend/Local Storage**: Relational **SQLite** database managed via the **Room ORM**. The schema is strictly normalized to the **4th Normal Form (4NF)** to ensure structured AI response parsing and eliminate data redundancy.
- **External Services**: Integration with Google Gemini API for LLM processing and Firebase for authentication and cloud sync.

### 2. Database Schema & Caching Strategy

To manage AI API costs and provide robust offline capabilities, the system relies heavily on local SQLite caching.

- **Entities**: The database contains entities such as `USER`, `SUBJECT`, `QUESTION`, and decoupled AI response tables (`AI_RESPONSE`, `RESPONSE_FORMULA`, `RESPONSE_ALTERNATIVE`, `RESPONSE_MISTAKE`).
- **Caching Mechanism**: Before dispatching an API call, the system generates a hash of the question string. If a cache hit occurs locally, the network request is bypassed, returning the stored response instantly.
- **Foreign Key Constraints**: Strict cascading deletions (`ON DELETE CASCADE`) are enforced to maintain relational integrity and prevent orphan records.

#### 3. API Integration and Concurrency

- **Networking**: `OkHttp` and `Retrofit` are used to manage HTTP connections to the Gemini API.
- **Thread Management**: All network requests and heavy database writes are offloaded to background threads using `ExecutorService`. The UI is safely updated via `runOnUiThread()`, completely resolving initial main-thread blocking issues (`NetworkOnMainThreadException`).
- **JSON Parsing & Markdown**: Responses from Gemini are parsed from JSON and passed through a specialized Markdown rendering engine within `AnswerActivity.java` to properly format mathematical syntax and structured text on the mobile view.

#### 4. Security & Compliance Measures

Given the target demographic (including minors), strict security and compliance standards are enforced:

- **COPPA & GDPR Compliance**: Age-gating during registration triggers a parental consent workflow for users under 13. Users have full rights to export or permanently delete their data (`TC50` compliance).
- **Data Encryption**: The user's Gemini API Key is stored securely using Android's `EncryptedSharedPreferences`, mitigating plaintext storage vulnerabilities identified during peer review.
- **Input Sanitization**: SQLite parameterized queries are used exclusively to block SQL injection attacks (`TC10`).

#### 5. Background Synchronization

A Background Worker thread monitors network state. When the device transitions from offline to an active Wi-Fi/cellular state, it scans the `QUESTION` table for entries with `sync_status = false`, securely transmits the queued payloads to the API, and updates the local interface upon receipt of the AI responses.
