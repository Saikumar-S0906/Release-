# Create Extra — device checks (you)

Code-side robustness for C-tools is in. Use this on a **real phone** when you have 10–15 minutes.

## Must-pass (crash zone)

1. Expand → **Upscale 4×** on a large gallery photo → finishes, no crash.
2. Tap **Generate**, then quickly pick another photo → no recycled-bitmap crash.
3. Double-tap **Generate** → one run, one AI credit.
4. Talking Photo → result → switch tool → still smooth.

## Should-pass (product)

5. Airplane mode → Scene / Remix → Offline UI.
6. AI credits = 0 → Generate → Low Credits.
7. HD credits = 0 → Save → HD limit message.
8. Face Swap / People → Decline consent → no generate.
9. Every Create → Tools tile opens the right screen.
10. One locale switch → no raw `create_extra_*` keys.

## Logcat tip

Filter: `AndroidRuntime` + `pixijoy`. Red stack = fail → note tool + steps.
