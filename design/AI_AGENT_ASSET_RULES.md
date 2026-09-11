# PixiJoy — Asset Rules for AI Coding/Design Agents

1. Never invent a new icon when an existing PixiJoy design-system icon exists.
2. Prefer vector/SVG for UI icons and brand marks.
3. Never use emojis as the only icon for core navigation or critical actions.
4. Do not embed user-facing UI text inside images.
5. Do not use unlicensed/copyright-risky meme templates, fonts, photos, stickers, or logos.
6. Keep creative content separate from UI resources.
7. All image-heavy content must have thumbnails and memory-aware loading.
8. Define light/dark variants where needed.
9. Every animation must have a reduced-motion/static state.
10. Every content image needs an accessible description when it conveys meaning.
11. Never load full-resolution originals for thumbnails.
12. Use centralized asset IDs rather than hardcoding file names throughout features.
13. Remote creative content must be cacheable, versioned, invalidatable, and removable.
14. Platform brand icons must comply with the destination platform's current branding requirements.
15. Asset dimensions/aspect ratios must be chosen by the design system, not by individual screens.
16. Keep generated AI content and system UI resources in separate packages/modules.
17. Do not package large AI models or template libraries into the base APK unless there is a deliberate size/performance reason.
18. Add placeholders/skeletons for every network-backed image and generation result.
19. Never block the UI while decoding large media.
20. The asset system must support localization, accessibility and RTL without duplicating entire screens.
