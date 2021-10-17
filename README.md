# Text-to-Call App

## Backend Local Setup

### Pre-requisites

- Install Node.js: https://nodejs.org/en/download/
- Install Heroku Command Line Interface: https://devcenter.heroku.com/articles/heroku-cli

### Download Instructions

Download the repository:

```
git clone https://github.com/isaacbock/text-to-call-app.git
```

### Install Dependencies

Execute the following command at the root of the repository:

```
npm install
```

### Set Necessary Environment Variables (required only for local development)

Set the required environment variables (obfuscated API keys) through a .env file at the root of the project.

1. Create a file named _.env_ at the root of the project.
2. Copy the contents of _.env.example_ into the newly created _.env_ file.
3. Enter your TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and GOOGLE_APPLICATION_CREDENTIALS.

## Heroku Backend: Node.js & Express.js

### Deployment

Commits pushed to this repository will automatically be deployed to Heroku.

### View Logs

To display the most recent 100 logs via the command line, execute:

```
heroku logs -a cse437s-phone
```

To display a running live-stream of logs via the command line, execute:

```
heroku logs -a cse437s-phone --tail
```
