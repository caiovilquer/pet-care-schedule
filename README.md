# pet-care-schedule

## Local fake email server

To capture reminder notifications during development a MailHog container is
provided. Start it with:

```bash
docker compose up -d mailhog
```

MailHog exposes the SMTP service on `localhost:1025` and a web UI on
`http://localhost:8025` where all sent mails can be inspected.
