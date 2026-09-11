# Board asset manifest

Convention: `res/drawable/board_<flow>_<role>.webp|png`
Roles: logo, hero, tile_*, result_*, poster_*, ba_before, ba_after

## Packed drawables

Hybrid board assets under `res/drawable/board_*` — see latest count via folder. Includes:

- Onboarding / BA / C00 / C04–C14 heroes  
- **C07 scene pack:** beach, city, mountains, cafe, studio, space, festival (+ base scene)  
- Rematch/media: b5, b6, b13–16, b18, b27, diff  
- Trust hubs: b9 avatar, b10 pro, b11 settings, b20 safety, b21 privacy, b23 credits, b25 notif, b28 offline  

Wiring: `BoardToolArt.optionImages` (1:1 with tool options), `BoardFlowArt` for host hubs.

## Source boards (active)

Canonical create/stickers/move boards are **REMATCH_*** only. See [BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md).

- `flows/01_Launch_and_Onboarding.png`
- `flows/02_Home_and_Discover.png`
- `flows/03_Photo_Video_Input.png`
- `flows/04_AI_Understanding.png`
- `flows/08_Save_Share_Community.png`
- `flows/09_Profile_Personalization.png`
- `flows/10_Pro_Subscription_Discovery.png`
- `flows/11_Settings_Help.png`
- `flows/12_End_to_End_Journey.png`
- `flows/15_Advanced_Photo_Editor.png`
- `flows/16_Multi_Photo_Collage.png`
- `flows/17_Export_Platform_Optimization.png`
- `flows/18_My_Creations_Drafts_History.png`
- `flows/19_AI_Generation_Recovery.png`
- `flows/20_AI_Safety_Moderation.png`
- `flows/21_Privacy_Center.png`
- `flows/22_Account_and_Authentication.png`
- `flows/23_AI_Credits.png`
- `flows/24_Subscription_Management.png`
- `flows/25_Notification_Center.png`
- `flows/26_Search_Templates.png`
- `flows/27_Community_UGC.png`
- `flows/28_Offline_Network_Recovery.png`
- `flows/29_Accessibility_localization.png`
- `flows/30_AppLifeCycle_Recovery.png`
- `flows/EXTRA_Sharing_and_Collaboration.png`
- `flows/additional/REMATCH_05_Make_it_Beautiful.png`
- `flows/additional/REMATCH_06_Make_it_Funny.png`
- `flows/additional/REMATCH_07_Tell_PixiJoy.png`
- `flows/additional/REMATCH_13_Sticker.png`
- `flows/additional/REMATCH_14_Move.png`
- `flows/additional/REMATCH_EXTRA_Different.png`
- `flows/additional/C00_Empty_Home_First_Creation.jpg`
- `flows/additional/C04_Remove_Background_Object.jpg`
- `flows/additional/C05_Restore_Old_Photo.jpg`
- `flows/additional/C06_Face_Swap_Recast.jpg`
- `flows/additional/C07_Change_Scene_Relocate.jpg`
- `flows/additional/C08_Add_or_Remove_People.jpg`
- `flows/additional/C09_AI_Portrait_Headshot.jpg`
- `flows/additional/C10_AI_Expand_Fill_Upscale.jpg`
- `flows/additional/C11_Talking_Photo.jpg`
- `flows/additional/C12_Greeting_Card_Festival.jpg`
- `flows/additional/C14_Remix_from_Community.jpg`

## Crop pipeline

1. Record phone-frame coords in `tools/board_assets/crops.json`.
2. `pip install pillow` then `python tools/board_assets/crop_export.py`.
3. Or generate stand-ins with Cursor GenerateImage (board PNG as reference).
