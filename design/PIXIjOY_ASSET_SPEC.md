# PixiJoy — Production Asset Inventory

## 1. Required branding
- Primary logo: horizontal, transparent, SVG.
- App icon: 1024px master + Android adaptive icon layers.
- Monochrome app icon for Android themed/monochrome icon support.
- Dark-background logo.
- Light-background logo.
- Small mark-only logo.
- Favicon/web icon if a web landing page is created.

## 2. UI icon system
Prefer vector icons or Material Symbols/custom SVGs. Do NOT use PNGs for ordinary UI icons.
Core groups:
- Navigation: home, create, explore, me, back, close, menu, search.
- Input: camera, gallery, file/import, multi-select, video.
- Creation intents: beautiful, funny, different, move, AI prompt.
- Editing: crop, adjust, filters, text, brush, erase, mask, layers, background, retouch.
- Output: save, download, share, remix, favorite, delete.
- Status: loading, success, warning, error, offline, locked/premium.
- Social: WhatsApp, Instagram, Snapchat, Facebook, Telegram, Android share.
- Media: play, pause, mute, trim, speed, loop.

## 3. AI-specific visual assets
- AI sparkle mark.
- AI generation shimmer.
- AI analyzing illustration.
- AI result state.
- AI safety/rejection illustration.
- AI failure/retry illustration.
- Empty AI state.
- "Try this" prompt chip visuals.
- AI assistant avatar/mark.
These should be subtle and premium, not cartoonish.

## 4. Creative content
This is the largest asset library:
- Meme templates.
- Sticker packs.
- Emoji/expression assets.
- GIF reaction assets.
- Photo effects.
- Backgrounds.
- AI styles.
- Frames.
- Text effects.
- Collage layouts.
- Reel/story templates.
- Birthday/wedding/travel/festival/social templates.
Use licensed, owned, commissioned, or generatively-created assets only.

## 5. Illustrations / empty states
Required:
- No creations.
- No drafts.
- No favorites.
- No templates.
- No network.
- AI unavailable.
- Generation failed.
- Unsupported media.
- Permission denied.
- Storage problem.
- Purchase failed.
- Account deletion confirmation.

## 6. Motion
Preferred format:
- Lottie JSON for small, deterministic UI animations.
- Compose/native animations for transitions.
- MP4/WebM only where actual video is required.
Required motion:
- AI generation.
- Success.
- Sticker pop.
- Before/after.
- Remix.
- Share success.
- Favorite.
Every animation needs a reduced-motion alternative.

## 7. Image export presets
User-facing presets:
- Original.
- Instagram Post.
- Instagram Story/Reel.
- WhatsApp Status.
- Snapchat.
- Square.
- Portrait.
- Landscape.
- Wallpaper.
Technical export profiles should remain hidden behind "More" or "Custom".

## 8. Camera/editor overlays
Need assets/components, not static images:
- Focus indicator.
- Exposure slider.
- Grid.
- Level indicator.
- Timer.
- Flash state.
- Front/back camera.
- Recording indicator.
- Crop guides.
- Before/after divider.
- Brush cursor.
- Selection handles.

## 9. Localization
Do not bake text into images unless unavoidable.
Provide:
- RTL-safe assets.
- Mirrored directional assets where required.
- Translation-safe layouts.
- Dynamic text overlays for memes/templates.

## 10. Accessibility
Each important icon needs:
- contentDescription/semantic label.
- 48dp minimum touch target.
- non-color-only state.
- scalable text.
- reduced-motion support.

## 11. AI model assets / ML resources
Potential resources:
- on-device segmentation model.
- face/landmark model where permitted.
- object detection model.
- image quality classifier.
- NSFW/safety classifier.
- prompt safety rules.
- thumbnail/downsampling pipeline.
Keep model files versioned and independently updateable where architecture permits.

## 12. Play Store assets
Prepare:
- App icon.
- Feature graphic.
- Phone screenshots.
- Tablet screenshots if supporting tablets.
- Promo video if used.
- Short description.
- Full description.
- Privacy policy URL.
- Data safety information.
- Content rating inputs.
- App access/demo credentials where required.

## 13. Asset naming convention
Use:
pj_ic_<name>.xml
pj_illustration_<name>.webp
pj_template_<category>_<id>.webp
pj_sticker_<pack>_<id>.webp
pj_lottie_<name>.json
pj_bg_<category>_<id>.webp

## 14. Do not store inside APK
Avoid putting huge mutable content libraries in res/drawable.
Remote/content-managed assets should use CDN/object storage + local cache.
Keep core UI assets in the APK; keep large templates, stickers, AI backgrounds and media packs remotely managed.
