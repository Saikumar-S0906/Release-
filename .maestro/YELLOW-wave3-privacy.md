# Wave 3 — Privacy / delete-all / export notif

## In app

- Privacy gate: `privacy_policy_link`, `privacy_terms_link`, `privacy_accept`
- Library footer: `library_privacy_link`, `library_terms_link`, `library_delete_all`, `library_reset_app`
- Export: channel name = localized Export; progress uses `studio_export_pct` / `studio_exporting`
- `POST_NOTIFICATIONS` requested on Export confirm (API 33+), not at cold start

## Maestro (optional)

```
maestro --device emulator-5556 test .maestro/chrome/privacy-legal-library.yaml
```

## Store (Wave 4)

Play Data safety form + content rating still console work — keep aligned with on-device claim.
