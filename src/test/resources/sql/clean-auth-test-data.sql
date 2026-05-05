TRUNCATE TABLE app_captcha_challenges,
    app_mfa_challenges,
    app_email_verifications,
    app_sessions,
    app_users
RESTART IDENTITY CASCADE;
