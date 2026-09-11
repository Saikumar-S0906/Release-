# Agent instructions — PixiJoy

Before changing navigation, routes, tabs, or create entry points:

1. Read `docs/NAVIGATION.md`
2. Look up the flow id in `docs/flows.json`
3. Follow `.cursor/rules/pixijoy-navigation.mdc`

Do not invent tabs. Chrome labels are **Home · Explore · Create · Community · My Stuff**.
Create tab is **Tools** (default) | **AI**. AI is not a sixth tab.

When auditing implementation, mark each `flows.json` id DONE / PARTIAL / MISSING with the route name as evidence.
