erDiagram
    %% Core Lookups
    SUBJECT {
        int subject_id PK
        string subject_name
    }
    REWARD_DICTIONARY {
        int reward_id PK
        string reward_type
        string reward_name
    }

    %% User & Links
    USER {
        int user_id PK
        string email
        string password_hash
        string education_level
        string ai_customization
        string ai_tone
        int xp_points
    }
    USER_SUBJECT {
        int user_id PK, FK
        int subject_id PK, FK
    }
    USER_REWARD {
        int user_id PK, FK
        int reward_id PK, FK
        timestamp earned_at
    }
    USER_SUBSCRIPTION {
        int sub_id PK
        int user_id FK
        string tier
        date valid_until
    }

    %% Questions & Practice
    QUESTION {
        int question_id PK
        int user_id FK
        int subject_id FK
        string content_text
        string image_path
        string identified_difficulty
        boolean is_bookmarked
        boolean sync_status
        timestamp created_at
    }
    PRACTICE_QUIZ {
        int quiz_id PK
        int user_id FK
        int source_question_id FK
        string question_type
        text quiz_content
        string student_answer
        boolean is_correct
        text instant_feedback
    }

    %% AI Responses & 4NF Multi-Valued Resolution
    AI_RESPONSE {
        int response_id PK
        int question_id FK
        text logical_steps
        string final_answer
        text simplified_explanation
    }
    RESPONSE_FORMULA {
        int formula_id PK
        int response_id FK
        string formula_name
    }
    RESPONSE_ALTERNATIVE {
        int alternative_id PK
        int response_id FK
        text alternative_text
    }
    RESPONSE_MISTAKE {
        int mistake_id PK
        int response_id FK
        text mistake_text
    }

    %% Notifications
    NOTIFICATION {
        int notification_id PK
        int user_id FK
        string notification_type
        text message
        boolean is_read
        timestamp created_at
    }

    %% Relationships
    USER ||--o{ USER_SUBJECT : "selects"
    SUBJECT ||--o{ USER_SUBJECT : "categorizes"
    USER ||--o{ QUESTION : "asks"
    SUBJECT ||--o{ QUESTION : "categorizes"
    USER ||--o{ PRACTICE_QUIZ : "takes"
    USER ||--o{ USER_REWARD : "earns"
    REWARD_DICTIONARY ||--o{ USER_REWARD : "awarded as"
    USER ||--o{ NOTIFICATION : "receives"
    USER ||--o{ USER_SUBSCRIPTION : "manages"

    QUESTION ||--o| AI_RESPONSE : "generates"
    QUESTION ||--o{ PRACTICE_QUIZ : "bases quiz on"

    AI_RESPONSE ||--o{ RESPONSE_FORMULA : "uses"
    AI_RESPONSE ||--o{ RESPONSE_ALTERNATIVE : "provides"
    AI_RESPONSE ||--o{ RESPONSE_MISTAKE : "identifies"erDiagram
    %% Core Lookups
    SUBJECT {
        int subject_id PK
        string subject_name
    }
    REWARD_DICTIONARY {
        int reward_id PK
        string reward_type
        string reward_name
    }

    %% User & Links
    USER {
        int user_id PK
        string email
        string password_hash
        string education_level
        string ai_customization
        string ai_tone
        int xp_points
    }
    USER_SUBJECT {
        int user_id PK, FK
        int subject_id PK, FK
    }
    USER_REWARD {
        int user_id PK, FK
        int reward_id PK, FK
        timestamp earned_at
    }
    USER_SUBSCRIPTION {
        int sub_id PK
        int user_id FK
        string tier
        date valid_until
    }

    %% Questions & Practice
    QUESTION {
        int question_id PK
        int user_id FK
        int subject_id FK
        string content_text
        string image_path
        string identified_difficulty
        boolean is_bookmarked
        boolean sync_status
        timestamp created_at
    }
    PRACTICE_QUIZ {
        int quiz_id PK
        int user_id FK
        int source_question_id FK
        string question_type
        text quiz_content
        string student_answer
        boolean is_correct
        text instant_feedback
    }

    %% AI Responses & 4NF Multi-Valued Resolution
    AI_RESPONSE {
        int response_id PK
        int question_id FK
        text logical_steps
        string final_answer
        text simplified_explanation
    }
    RESPONSE_FORMULA {
        int formula_id PK
        int response_id FK
        string formula_name
    }
    RESPONSE_ALTERNATIVE {
        int alternative_id PK
        int response_id FK
        text alternative_text
    }
    RESPONSE_MISTAKE {
        int mistake_id PK
        int response_id FK
        text mistake_text
    }

    %% Notifications
    NOTIFICATION {
        int notification_id PK
        int user_id FK
        string notification_type
        text message
        boolean is_read
        timestamp created_at
    }

    %% Relationships
    USER ||--o{ USER_SUBJECT : "selects"
    SUBJECT ||--o{ USER_SUBJECT : "categorizes"
    USER ||--o{ QUESTION : "asks"
    SUBJECT ||--o{ QUESTION : "categorizes"
    USER ||--o{ PRACTICE_QUIZ : "takes"
    USER ||--o{ USER_REWARD : "earns"
    REWARD_DICTIONARY ||--o{ USER_REWARD : "awarded as"
    USER ||--o{ NOTIFICATION : "receives"
    USER ||--o{ USER_SUBSCRIPTION : "manages"

    QUESTION ||--o| AI_RESPONSE : "generates"
    QUESTION ||--o{ PRACTICE_QUIZ : "bases quiz on"

    AI_RESPONSE ||--o{ RESPONSE_FORMULA : "uses"
    AI_RESPONSE ||--o{ RESPONSE_ALTERNATIVE : "provides"
    AI_RESPONSE ||--o{ RESPONSE_MISTAKE : "identifies"