# Gmail Client

A Spring Boot web application that lets you read, compose, and reply to Gmail in a clean browser UI.

## Features

- Sign in with Google (OAuth2)
- View inbox and other labels (Sent, Drafts, Spam, Trash, custom labels)
- Read full email messages (HTML and plain text)
- Compose new emails
- Reply to emails with proper threading
- Responsive UI built with Bootstrap 5

## Prerequisites

- Java 17+
- Maven 3.9+
- A Google Cloud project with the Gmail API enabled and OAuth2 credentials created

## Google Cloud Setup

### 1. Create a project and enable the Gmail API

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project (or select an existing one)
3. Navigate to **APIs & Services → Library**, search for **Gmail API**, and click **Enable**

### 2. Configure the OAuth consent screen

1. Go to **APIs & Services → OAuth consent screen**
2. Choose **External** → **Create**
3. Fill in the app name, support email, and developer contact email
4. Add the following scopes:
   - `https://www.googleapis.com/auth/gmail.readonly`
   - `https://www.googleapis.com/auth/gmail.send`
   - `https://www.googleapis.com/auth/gmail.modify`
   - `openid`, `email`, `profile`
5. Under **Test users**, add the Gmail address(es) you want to sign in with
6. Save

### 3. Create OAuth2 credentials

1. Go to **APIs & Services → Credentials → + Create Credentials → OAuth client ID**
2. Application type: **Web application**
3. Add the redirect URI for your environment:
   - Local: `http://localhost:8080/login/oauth2/code/google`
   - Render: `https://your-app.onrender.com/login/oauth2/code/google`
4. Click **Create** and copy the **Client ID** and **Client Secret**

> **Note:** While the app is in Testing mode only the Google accounts you listed as test users can sign in. This is fine for personal use — no need to go through Google's verification process.

## Running Locally

```bash
export GOOGLE_CLIENT_ID=your_client_id
export GOOGLE_CLIENT_SECRET=your_client_secret
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) and sign in with Google.

## Running with Docker

```bash
docker build -t gmail-client .
docker run -p 8080:8080 \
  -e GOOGLE_CLIENT_ID=your_client_id \
  -e GOOGLE_CLIENT_SECRET=your_client_secret \
  gmail-client
```

## Deploying to Render

1. Push this repo to GitHub
2. In Render → **New Web Service** → connect the repo
3. Set **Environment** to `Docker`
4. Add environment variables:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
5. Add your Render URL as an authorised redirect URI in Google Cloud Console:
   `https://your-app.onrender.com/login/oauth2/code/google`
6. Deploy

Render automatically injects a `PORT` environment variable; the Dockerfile passes it to Spring Boot at startup.

## Configuration

All configuration is in `src/main/resources/application.yml`. The two required values are read from environment variables:

| Environment Variable   | Description              |
|------------------------|--------------------------|
| `GOOGLE_CLIENT_ID`     | OAuth2 client ID         |
| `GOOGLE_CLIENT_SECRET` | OAuth2 client secret     |

## Project Structure

```
src/main/java/com/example/gmailclient/
├── GmailClientApplication.java
├── config/
│   └── SecurityConfig.java          # OAuth2 + route protection
├── controller/
│   ├── HomeController.java           # / redirect
│   ├── InboxController.java          # /inbox
│   ├── EmailController.java          # /email/{id}
│   └── ComposeController.java        # /compose, /send
├── service/
│   └── GmailService.java             # Gmail REST API calls
└── model/
    ├── EmailSummary.java
    ├── EmailMessage.java
    └── LabelInfo.java

src/main/resources/templates/
├── fragments/layout.html             # Shared navbar + sidebar
├── index.html                        # Sign-in page
├── inbox.html
├── email-view.html
└── compose.html
```

## How This Was Built

This app was generated entirely using [Claude Code](https://claude.ai/code) with the following prompts:

---

**Prompt 1 — Generate the app**
> create a spring boot app which is web client for gmail.

---

**Prompt 2 — Add Docker support**
> add a dockerfile which makes it easy to deploy to render

---

**Prompt 3 — Documentation**
> update the readme file

---

## Tech Stack

- Spring Boot 3.2
- Spring Security OAuth2 Client
- Spring Web MVC + Thymeleaf
- Bootstrap 5 (CDN)
- Gmail REST API via Spring `RestClient`
- Jakarta Mail (MIME construction for sending)
