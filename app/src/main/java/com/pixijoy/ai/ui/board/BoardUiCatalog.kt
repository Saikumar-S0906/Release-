package com.pixijoy.ai.ui.board

import com.pixijoy.ai.ui.board.ctools.BoardCTool

/**
 * Parallel board-faithful UI catalog.
 *
 * [Status.Done] means **UI Done / soft AI** — Compose path is wired; generate /
 * OAuth / sync / payments are simulated. It is **not** L3 pixel lock and **not**
 * live generative backends. See docs/BOARD_UI_CATALOG.md and docs/BOARD_RELEASE_HYGIENE.md.
 */
data class BoardScreenEntry(
    val flow: Int,
    val id: String,
    val title: String,
    val kind: Kind = Kind.Happy,
    val status: Status = Status.Pending,
    val boardFile: String = ""
) {
    enum class Kind { Happy, Alt, Edge, Tip, Shared }

    enum class Status {
        Pending,
        InProgress,

        /** UI Done / soft AI — see [releaseLabel]. */
        Done;

        /** Honest label for indexes, placeholders, and release notes. */
        val releaseLabel: String
            get() = when (this) {
                Pending -> "Pending"
                InProgress -> "In progress"
                Done -> "UI Done / soft AI"
            }
    }
}

object BoardUiCatalog {
    val all: List<BoardScreenEntry> = buildList {
        // Flow 1
        addAll(
            listOf(
                BoardScreenEntry(1, "b1_splash", "Splash", boardFile = "01_Launch_and_Onboarding.png"),
                BoardScreenEntry(1, "b1_welcome", "Welcome"),
                BoardScreenEntry(1, "b1_features", "Key Features"),
                BoardScreenEntry(1, "b1_for_everyone", "For Everyone"),
                BoardScreenEntry(1, "b1_permissions", "Photos Permission"),
                BoardScreenEntry(1, "b1_signup", "Optional Sign Up"),
                BoardScreenEntry(1, "b1_ready", "Ready to Create"),
                BoardScreenEntry(1, "b1_skip", "Skip Onboarding", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(1, "b1_perm_denied", "Permission Denied", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(1, "b1_limited", "Limited Photo Access", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(1, "b1_signup_later", "Sign Up Later", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(1, "b1_returning", "Returning User", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(1, "b1_whats_new", "What's New", BoardScreenEntry.Kind.Alt)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Flow 2
        addAll(
            listOf(
                BoardScreenEntry(2, "b2_home", "Home Default", boardFile = "02_Home_and_Discover.png"),
                BoardScreenEntry(2, "b2_home_scroll", "Home Scrolled"),
                BoardScreenEntry(2, "b2_discover", "Discover Main"),
                BoardScreenEntry(2, "b2_discover_cat", "Discover Category"),
                BoardScreenEntry(2, "b2_template", "Template Detail"),
                BoardScreenEntry(2, "b2_template_create", "Customize Template"),
                BoardScreenEntry(2, "b2_template_result", "Result & Share"),
                BoardScreenEntry(2, "b2_search", "Search"),
                BoardScreenEntry(2, "b2_trending", "Trending"),
                BoardScreenEntry(2, "b2_collections", "Collections"),
                BoardScreenEntry(2, "b2_seasons", "Seasons & Events"),
                BoardScreenEntry(2, "b2_creators", "Creators"),
                BoardScreenEntry(2, "b2_favorites", "Favorites"),
                BoardScreenEntry(2, "b2_recent", "Recently Used")
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // C00
        addAll(
            listOf(
                BoardScreenEntry(100, "b_c00_empty", "Empty Home", boardFile = "C00_Empty_Home_First_Creation.jpg"),
                BoardScreenEntry(100, "b_c00_taste", "Taste Personalization"),
                BoardScreenEntry(100, "b_c00_skip_taste", "Skip Taste", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(100, "b_c00_discover", "C00 Discover"),
                BoardScreenEntry(100, "b_c00_funny", "Funny Category"),
                BoardScreenEntry(100, "b_c00_generating", "Generating"),
                BoardScreenEntry(100, "b_c00_first", "First Creation"),
                BoardScreenEntry(100, "b_c00_guest_gate", "Save to Account", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(100, "b_c00_sample", "Sample Photo", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(100, "b_c00_perm_denied", "Permission Denied", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(100, "b_c00_guest", "Guest Limits", BoardScreenEntry.Kind.Alt)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Flows 3–4
        addAll(
            listOf(
                BoardScreenEntry(3, "b3_open", "Open Add Photo", boardFile = "03_Photo_Video_Input.png"),
                BoardScreenEntry(3, "b3_chooser", "Media Chooser"),
                BoardScreenEntry(3, "b3_camera", "Take a Photo"),
                BoardScreenEntry(3, "b3_gallery", "Gallery"),
                BoardScreenEntry(3, "b3_preview", "Preview & Edit"),
                BoardScreenEntry(3, "b3_multi", "Multi Purpose"),
                BoardScreenEntry(3, "b3_video", "Video Selection"),
                BoardScreenEntry(3, "b3_trim", "Video Trim", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(3, "b3_proceed", "Proceed to AI"),
                BoardScreenEntry(3, "b3_camera_denied", "Camera Denied", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_photos_denied", "Photos Denied", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_limited", "Limited Access", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_no_photos", "No Photos", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_unsupported", "Unsupported File", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_too_large", "File Too Large", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(3, "b3_corrupt", "Corrupt File", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        addAll(
            listOf(
                BoardScreenEntry(4, "b4_selected", "Photo Selected", boardFile = "04_AI_Understanding.png"),
                BoardScreenEntry(4, "b4_analyzing", "AI Analyzing"),
                BoardScreenEntry(4, "b4_understanding", "Understanding Result"),
                BoardScreenEntry(4, "b4_result", "Understanding Result Alt"),
                BoardScreenEntry(4, "b4_suggestions", "Suggestions"),
                BoardScreenEntry(4, "b4_choose_path", "Choose Path"),
                BoardScreenEntry(4, "b4_preview", "Quick Preview"),
                BoardScreenEntry(4, "b4_start", "Start Creating"),
                BoardScreenEntry(4, "b4_no_subject", "No Subject", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_multi_subject", "Multiple Subjects", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_low_quality", "Low Quality", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_no_face", "No Face", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_unsupported", "Unsupported Image", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_offline", "Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(4, "b4_safety", "Safety Check", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Creation REMATCH
        addAll(
            listOf(
                BoardScreenEntry(5, "b_create_hub", "Create Hub"),
                BoardScreenEntry(5, "b5_choose", "Beautiful Choose", boardFile = "REMATCH_05_Make_it_Beautiful.png"),
                BoardScreenEntry(5, "b5_beautiful", "Make it Beautiful"),
                BoardScreenEntry(5, "b5_adjust", "Beautiful Adjust"),
                BoardScreenEntry(5, "b5_processing", "Beautiful Processing"),
                BoardScreenEntry(5, "b5_result", "Beautiful Result"),
                BoardScreenEntry(5, "b5_variations", "Beautiful Variations"),
                BoardScreenEntry(5, "b5_edit", "Beautiful Edit"),
                BoardScreenEntry(5, "b5_ready", "Beautiful Ready"),
                BoardScreenEntry(5, "b5_offline", "Beautiful Offline"),
                BoardScreenEntry(5, "b5_low_credits", "Beautiful Low Credits"),
                BoardScreenEntry(5, "b5_safety", "Beautiful Safety"),
                BoardScreenEntry(6, "b6_choose", "Funny Choose", boardFile = "REMATCH_06_Make_it_Funny.png"),
                BoardScreenEntry(6, "b6_funny", "Make it Funny"),
                BoardScreenEntry(6, "b6_customize", "Funny Customize"),
                BoardScreenEntry(6, "b6_processing", "Funny Processing"),
                BoardScreenEntry(6, "b6_result", "Funny Result"),
                BoardScreenEntry(6, "b6_variations", "Funny Variations"),
                BoardScreenEntry(6, "b6_share", "Funny Share"),
                BoardScreenEntry(6, "b6_offline", "Funny Offline"),
                BoardScreenEntry(6, "b6_low_credits", "Funny Low Credits"),
                BoardScreenEntry(6, "b6_safety", "Funny Safety"),
                BoardScreenEntry(7, "b7_sheet", "Tell Sheet", boardFile = "REMATCH_07_Tell_PixiJoy.png"),
                BoardScreenEntry(7, "b7_tell", "Tell PixiJoy"),
                BoardScreenEntry(7, "b7_input", "Tell Input Method"),
                BoardScreenEntry(7, "b7_request", "Tell Request"),
                BoardScreenEntry(7, "b7_confirm", "Tell Confirm"),
                BoardScreenEntry(7, "b7_understanding", "Tell Understanding"),
                BoardScreenEntry(7, "b7_generating", "Tell Generating"),
                BoardScreenEntry(7, "b7_result", "Tell Result"),
                BoardScreenEntry(7, "b7_next", "Tell What's Next"),
                BoardScreenEntry(7, "b7_offline", "Tell Offline"),
                BoardScreenEntry(7, "b7_low_credits", "Tell Low Credits"),
                BoardScreenEntry(7, "b7_safety", "Tell Safety"),
                BoardScreenEntry(0, "b_diff_choose", "Different · Select Photo", boardFile = "REMATCH_EXTRA_Different.png"),
                BoardScreenEntry(0, "b_different", "Make it Different"),
                BoardScreenEntry(0, "b_diff_options", "Different · AI Styles"),
                BoardScreenEntry(0, "b_diff_adjust", "Different · Customize"),
                BoardScreenEntry(0, "b_diff_processing", "Different · Processing"),
                BoardScreenEntry(0, "b_diff_result", "Different · Result"),
                BoardScreenEntry(0, "b_diff_variations", "Different · Variations"),
                BoardScreenEntry(0, "b_diff_share", "Different · Saved"),
                BoardScreenEntry(0, "b_diff_offline", "Different · Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(0, "b_diff_low_credits", "Different · Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(0, "b_diff_safety", "Different · Safety", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Shared gen kit
        addAll(
            listOf(
                BoardScreenEntry(19, "b19_start", "Start Generation", BoardScreenEntry.Kind.Shared, boardFile = "19_AI_Generation_Recovery.png"),
                BoardScreenEntry(19, "b19_generating", "Generating", BoardScreenEntry.Kind.Shared),
                BoardScreenEntry(19, "b19_analyzing", "Analyzing", BoardScreenEntry.Kind.Shared),
                BoardScreenEntry(19, "b19_almost", "Almost Ready", BoardScreenEntry.Kind.Shared),
                BoardScreenEntry(19, "b19_success", "Success", BoardScreenEntry.Kind.Shared),
                BoardScreenEntry(19, "b19_canceled", "Canceled", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(19, "b19_offline", "Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_busy", "Busy", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_timeout", "Timeout", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_unsupported", "Unsupported", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_safety", "Safety Filter", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_low_credits", "Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(19, "b19_failed", "Failed", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // C-tools C04–C14 (Wave 3)
        BoardCTool.entries.forEach { tool ->
            val steps = buildList {
                add("intro" to "Intro")
                add("photo" to "Pick Photo")
                if (tool.needsConsent) {
                    add("consent" to "Consent")
                    add("consent_denied" to "Consent Denied")
                }
                add("options" to "Options")
                add("adjust" to "Adjust")
                if (tool == BoardCTool.RemoveBg) {
                    add("processing" to "AI Processing")
                    add("refine" to "Refine Edges")
                }
                if (tool == BoardCTool.Restore) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.FaceSwap) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.People) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Scene) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Portrait) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Talking) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Greeting) {
                    add("edit" to "Edit Text")
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Remix) {
                    add("processing" to "AI Processing")
                }
                if (tool == BoardCTool.Expand) {
                    add("filters" to "Filters")
                    add("processing" to "AI Processing")
                    add("erase" to "Generative Erase")
                }
                add("result" to "Result")
                add("variations" to "Variations")
                if (tool == BoardCTool.RemoveBg) add("erase" to "Remove Object")
                if (tool == BoardCTool.Restore) add("edit" to "Edit Photo")
                add("share" to "Share")
                add("offline" to "Offline")
                add("low_credits" to "Low Credits")
                add("safety" to "Safety")
            }
            steps.forEachIndexed { index, (step, title) ->
                val id = if (step == "intro") tool.prefix else "${tool.prefix}_$step"
                val kind = when (step) {
                    "consent_denied", "offline", "low_credits", "safety" -> BoardScreenEntry.Kind.Edge
                    "consent" -> BoardScreenEntry.Kind.Tip
                    else -> BoardScreenEntry.Kind.Happy
                }
                add(
                    BoardScreenEntry(
                        flow = tool.flow,
                        id = id,
                        title = "${tool.name} · $title",
                        kind = kind,
                        status = BoardScreenEntry.Status.Done,
                        boardFile = if (index == 0) tool.boardFile else ""
                    )
                )
            }
        }
        // Wave 4 — media studios 13–17
        addAll(
            listOf(
                BoardScreenEntry(13, "b13_photo", "Stickers Photo", boardFile = "REMATCH_13_Sticker.png"),
                BoardScreenEntry(13, "b13_cutout", "Cutout"),
                BoardScreenEntry(13, "b13_refine", "Refine"),
                BoardScreenEntry(13, "b13_express", "Expression / Text"),
                BoardScreenEntry(13, "b13_pack", "Pack"),
                BoardScreenEntry(13, "b13_preview", "Preview"),
                BoardScreenEntry(13, "b13_share", "Share Stickers"),
                BoardScreenEntry(13, "b13_offline", "Stickers Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(13, "b13_low_credits", "Stickers Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(13, "b13_safety", "Stickers Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(14, "b14_input", "Move Input", boardFile = "REMATCH_14_Move.png"),
                BoardScreenEntry(14, "b14_media", "Move Media"),
                BoardScreenEntry(14, "b14_motion", "Motion Style"),
                BoardScreenEntry(14, "b14_customize", "Customize"),
                BoardScreenEntry(14, "b14_gen", "Generating"),
                BoardScreenEntry(14, "b14_preview", "Move Preview"),
                BoardScreenEntry(14, "b14_save", "Save / Share"),
                BoardScreenEntry(14, "b14_stories", "Enjoy Everywhere"),
                BoardScreenEntry(14, "b14_offline", "Move Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(14, "b14_low_credits", "Move Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(14, "b14_safety", "Move Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(15, "b15_open", "Editor Open", boardFile = "15_Advanced_Photo_Editor.png"),
                BoardScreenEntry(15, "b15_workspace", "Workspace"),
                BoardScreenEntry(15, "b15_crop", "Crop"),
                BoardScreenEntry(15, "b15_adjust", "Adjust"),
                BoardScreenEntry(15, "b15_filters", "Filters"),
                BoardScreenEntry(15, "b15_text", "Text / Stickers"),
                BoardScreenEntry(15, "b15_more", "More Tools"),
                BoardScreenEntry(15, "b15_save", "Editor Save"),
                BoardScreenEntry(15, "b15_offline", "Editor Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(15, "b15_low_credits", "Editor Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(15, "b15_safety", "Editor Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(16, "b16_select", "Collage Select", boardFile = "16_Multi_Photo_Collage.png"),
                BoardScreenEntry(16, "b16_purpose", "12 Purposes"),
                BoardScreenEntry(16, "b16_arrange", "Arrange"),
                BoardScreenEntry(16, "b16_creating", "AI Creating"),
                BoardScreenEntry(16, "b16_preview", "Collage Preview"),
                BoardScreenEntry(16, "b16_save", "Collage Save"),
                BoardScreenEntry(16, "b16_end", "Collage End Result"),
                BoardScreenEntry(16, "b16_offline", "Collage Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(16, "b16_low_credits", "Collage Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(16, "b16_safety", "Collage Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(17, "b17_open", "Export Open", boardFile = "17_Export_Platform_Optimization.png"),
                BoardScreenEntry(17, "b17_platform", "Platform"),
                BoardScreenEntry(17, "b17_optimize", "Auto Optimize"),
                BoardScreenEntry(17, "b17_preview", "Export Preview"),
                BoardScreenEntry(17, "b17_advanced", "Advanced"),
                BoardScreenEntry(17, "b17_staging", "Export / Share"),
                BoardScreenEntry(17, "b17_exporting", "Exporting"),
                BoardScreenEntry(17, "b17_done", "Export Done"),
                BoardScreenEntry(17, "b17_offline", "Export Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(17, "b17_low_credits", "Export Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(17, "b17_safety", "Export Safety", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Wave 5 — library / share / search
        addAll(
            listOf(
                BoardScreenEntry(8, "b8_ready", "Save Ready", boardFile = "08_Save_Share_Community.png"),
                BoardScreenEntry(8, "b8_preview", "Preview & Edit"),
                BoardScreenEntry(8, "b8_saved", "Saved"),
                BoardScreenEntry(8, "b8_share", "Share Sheet"),
                BoardScreenEntry(8, "b8_publish", "Publish"),
                BoardScreenEntry(8, "b8_live", "Live Post"),
                BoardScreenEntry(8, "b8_manage", "Manage"),
                BoardScreenEntry(8, "b8_offline", "Share Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(8, "b8_low_credits", "Share Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(8, "b8_safety", "Share Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(0, "b_collab_select", "Collab Select", boardFile = "EXTRA_Sharing_and_Collaboration.png"),
                BoardScreenEntry(0, "b_collab_share", "Share Creation"),
                BoardScreenEntry(0, "b_collab_people", "People"),
                BoardScreenEntry(0, "b_collab_message", "Message"),
                BoardScreenEntry(0, "b_collab_perms", "Permissions"),
                BoardScreenEntry(0, "b_collab_ready", "Collab Ready"),
                BoardScreenEntry(0, "b_collab_album", "Shared Album"),
                BoardScreenEntry(0, "b_collab_activity", "Activity"),
                BoardScreenEntry(0, "b_collab_together", "Together"),
                BoardScreenEntry(0, "b_collab_offline", "Collab Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(0, "b_collab_low_credits", "Collab Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(0, "b_collab_safety", "Collab Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(18, "b18_browse", "Creations Browse", boardFile = "18_My_Creations_Drafts_History.png"),
                BoardScreenEntry(18, "b18_detail", "Creation Detail"),
                BoardScreenEntry(18, "b18_more", "More Sheet"),
                BoardScreenEntry(18, "b18_versions", "Versions"),
                BoardScreenEntry(18, "b18_album", "Albums"),
                BoardScreenEntry(18, "b18_search", "Creations Search"),
                BoardScreenEntry(18, "b18_trash", "Trash"),
                BoardScreenEntry(18, "b18_empty", "Creations Empty", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(18, "b18_offline", "Creations Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(18, "b18_low_credits", "Creations Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(18, "b18_safety", "Creations Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(26, "b26_open", "Search Open", boardFile = "26_Search_Templates.png"),
                BoardScreenEntry(26, "b26_search", "Search"),
                BoardScreenEntry(26, "b26_results", "Results"),
                BoardScreenEntry(26, "b26_filters", "Filters"),
                BoardScreenEntry(26, "b26_detail", "Template Detail"),
                BoardScreenEntry(26, "b26_customize", "Customize"),
                BoardScreenEntry(26, "b26_saved", "Template Saved"),
                BoardScreenEntry(26, "b26_more", "More Like This"),
                BoardScreenEntry(26, "b26_empty", "Search Empty", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(26, "b26_offline", "Search Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(26, "b26_low_credits", "Search Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(26, "b26_safety", "Search Safety", BoardScreenEntry.Kind.Edge)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Wave 6 — me / trust
        addAll(
            listOf(
                BoardScreenEntry(9, "b9_open", "Profile", boardFile = "09_Profile_Personalization.png"),
                BoardScreenEntry(9, "b9_edit", "Edit Profile"),
                BoardScreenEntry(9, "b9_theme", "Theme / Badges"),
                BoardScreenEntry(9, "b9_albums", "Albums"),
                BoardScreenEntry(9, "b9_favorites", "Favorites"),
                BoardScreenEntry(9, "b9_privacy", "Privacy Levels"),
                BoardScreenEntry(9, "b9_notifs", "Profile Notifs"),
                BoardScreenEntry(9, "b9_updated", "Profile Updated"),
                BoardScreenEntry(9, "b9_offline", "Profile Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(9, "b9_low_credits", "Profile Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(9, "b9_safety", "Profile Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(10, "b10_discover", "Pro Discover", boardFile = "10_Pro_Subscription_Discovery.png"),
                BoardScreenEntry(10, "b10_compare", "Compare Plans"),
                BoardScreenEntry(10, "b10_benefits", "Benefits"),
                BoardScreenEntry(10, "b10_pay", "Pay"),
                BoardScreenEntry(10, "b10_confirm", "Confirm"),
                BoardScreenEntry(10, "b10_welcome", "Pro Welcome"),
                BoardScreenEntry(10, "b10_inapp", "In-app Pro"),
                BoardScreenEntry(10, "b10_manage", "Manage Pro"),
                BoardScreenEntry(10, "b10_offline", "Pro Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(10, "b10_low_credits", "Pro Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(10, "b10_safety", "Pro Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(11, "b11_hub", "Settings Menu", boardFile = "11_Settings_Help.png"),
                BoardScreenEntry(11, "b11_home", "Settings Home", boardFile = "11_Settings_Help.png"),
                BoardScreenEntry(11, "b11_account", "Account"),
                BoardScreenEntry(11, "b11_delete", "Delete Account", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(11, "b11_prefs", "Preferences"),
                BoardScreenEntry(11, "b11_notifs", "Settings Notifs"),
                BoardScreenEntry(11, "b11_storage", "Storage"),
                BoardScreenEntry(11, "b11_language", "Language"),
                BoardScreenEntry(11, "b11_appearance", "Appearance"),
                BoardScreenEntry(11, "b11_help", "Help"),
                BoardScreenEntry(11, "b11_article", "Help Article"),
                BoardScreenEntry(11, "b11_offline", "Settings Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(11, "b11_low_credits", "Settings Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(11, "b11_safety", "Settings Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(20, "b20_input", "Safety Input", boardFile = "20_AI_Safety_Moderation.png"),
                BoardScreenEntry(20, "b20_check", "Safety Check"),
                BoardScreenEntry(20, "b20_allowed", "Allowed"),
                BoardScreenEntry(20, "b20_blocked", "Not Allowed", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(20, "b20_modify", "Modify"),
                BoardScreenEntry(20, "b20_generating", "Safe Generating"),
                BoardScreenEntry(20, "b20_result", "Safe Result"),
                BoardScreenEntry(20, "b20_report", "Report"),
                BoardScreenEntry(21, "b21_hub", "Privacy Hub", boardFile = "21_Privacy_Center.png"),
                BoardScreenEntry(21, "b21_photos", "Photos Privacy"),
                BoardScreenEntry(21, "b21_data", "Data"),
                BoardScreenEntry(21, "b21_personalization", "Personalization"),
                BoardScreenEntry(21, "b21_download", "Download/Delete"),
                BoardScreenEntry(21, "b21_confirm", "Confirm Delete", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(21, "b21_done", "Privacy Done"),
                BoardScreenEntry(22, "b22_welcome", "Auth Welcome", boardFile = "22_Account_and_Authentication.png"),
                BoardScreenEntry(22, "b22_methods", "Auth Methods"),
                BoardScreenEntry(22, "b22_oauth", "OAuth"),
                BoardScreenEntry(22, "b22_profile", "Auth Profile"),
                BoardScreenEntry(22, "b22_perms", "Optional Perms"),
                BoardScreenEntry(22, "b22_ready", "Auth Ready"),
                BoardScreenEntry(22, "b22_manage", "Manage Account"),
                BoardScreenEntry(22, "b22_devices", "Multi-device"),
                BoardScreenEntry(23, "b23_hub", "Credits Profile Entry", boardFile = "23_AI_Credits.png"),
                BoardScreenEntry(23, "b23_dash", "Credits Dashboard"),
                BoardScreenEntry(23, "b23_costs", "Cost Table"),
                BoardScreenEntry(23, "b23_packs", "Packs"),
                BoardScreenEntry(23, "b23_pay", "Credits Pay"),
                BoardScreenEntry(23, "b23_added", "Credits Added"),
                BoardScreenEntry(23, "b23_use", "Use Credits"),
                BoardScreenEntry(23, "b23_history", "Credits History"),
                BoardScreenEntry(24, "b24_plans", "Sub Plans", boardFile = "24_Subscription_Management.png"),
                BoardScreenEntry(24, "b24_choose", "Choose Plan"),
                BoardScreenEntry(24, "b24_matrix", "Feature Matrix"),
                BoardScreenEntry(24, "b24_pay", "Sub Pay"),
                BoardScreenEntry(24, "b24_welcome", "Sub Welcome"),
                BoardScreenEntry(24, "b24_manage", "Sub Manage"),
                BoardScreenEntry(24, "b24_billing", "Billing"),
                BoardScreenEntry(24, "b24_cancel", "Cancel Survey", BoardScreenEntry.Kind.Alt),
                BoardScreenEntry(25, "b25_hub", "Notif Home Entry", boardFile = "25_Notification_Center.png"),
                BoardScreenEntry(25, "b25_center", "Notif Center"),
                BoardScreenEntry(25, "b25_filtered", "Notif Filtered"),
                BoardScreenEntry(25, "b25_detail", "Notif Detail"),
                BoardScreenEntry(25, "b25_manage", "Notif Manage"),
                BoardScreenEntry(25, "b25_settings", "Notif Settings"),
                BoardScreenEntry(25, "b25_push", "Push Preview"),
                BoardScreenEntry(25, "b25_empty", "Notif Empty", BoardScreenEntry.Kind.Alt)
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
        // Wave 7 — social / recovery
        addAll(
            listOf(
                BoardScreenEntry(12, "b12_intro", "E2E Journey", boardFile = "12_End_to_End_Journey.png"),
                BoardScreenEntry(12, "b12_welcome", "Tour Welcome"),
                BoardScreenEntry(12, "b12_signin", "Tour Sign In"),
                BoardScreenEntry(12, "b12_home", "Tour Home"),
                BoardScreenEntry(12, "b12_create", "Tour Create"),
                BoardScreenEntry(12, "b12_edit", "Tour Edit"),
                BoardScreenEntry(12, "b12_share", "Tour Share"),
                BoardScreenEntry(12, "b12_community", "Tour Community"),
                BoardScreenEntry(12, "b12_pro", "Tour Pro"),
                BoardScreenEntry(12, "b12_offline", "Tour Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(12, "b12_low_credits", "Tour Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(12, "b12_safety", "Tour Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(27, "b27_feed", "Community Feed", boardFile = "27_Community_UGC.png"),
                BoardScreenEntry(27, "b27_trending", "Trending"),
                BoardScreenEntry(27, "b27_categories", "Categories"),
                BoardScreenEntry(27, "b27_post", "Post"),
                BoardScreenEntry(27, "b27_creator", "Creator"),
                BoardScreenEntry(27, "b27_people", "Find People"),
                BoardScreenEntry(27, "b27_share", "Community Share"),
                BoardScreenEntry(27, "b27_guidelines", "Guidelines"),
                BoardScreenEntry(27, "b27_creators", "Creators"),
                BoardScreenEntry(27, "b27_offline", "Community Offline", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(27, "b27_low_credits", "Community Low Credits", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(27, "b27_safety", "Community Safety", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(28, "b28_banner", "Offline Banner", boardFile = "28_Offline_Network_Recovery.png"),
                BoardScreenEntry(28, "b28_modal", "Offline Modal"),
                BoardScreenEntry(28, "b28_edit", "Local Edit"),
                BoardScreenEntry(28, "b28_saved", "Saved Offline"),
                BoardScreenEntry(28, "b28_online", "Back Online"),
                BoardScreenEntry(28, "b28_syncing", "Syncing"),
                BoardScreenEntry(28, "b28_synced", "Synced"),
                BoardScreenEntry(28, "b28_failed", "Sync Failed", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(29, "b29_home", "A11y Home", boardFile = "29_Accessibility_localization.png"),
                BoardScreenEntry(29, "b29_settings", "A11y Settings"),
                BoardScreenEntry(29, "b29_a11y", "Accessibility"),
                BoardScreenEntry(29, "b29_language", "Language"),
                BoardScreenEntry(29, "b29_appearance", "Appearance"),
                BoardScreenEntry(29, "b29_voice", "Voice"),
                BoardScreenEntry(29, "b29_save", "A11y Save"),
                BoardScreenEntry(29, "b29_localized", "Localized ES"),
                BoardScreenEntry(30, "b30_home", "Lifecycle Hub", boardFile = "30_AppLifeCycle_Recovery.png"),
                BoardScreenEntry(30, "b30_bg", "Background"),
                BoardScreenEntry(30, "b30_welcome", "Welcome Back"),
                BoardScreenEntry(30, "b30_update", "Update"),
                BoardScreenEntry(30, "b30_restart", "Restart"),
                BoardScreenEntry(30, "b30_crash", "Crash Recovery", BoardScreenEntry.Kind.Edge),
                BoardScreenEntry(30, "b30_device", "Device Restart"),
                BoardScreenEntry(30, "b30_integrity", "Data Integrity")
            ).map { it.copy(status = BoardScreenEntry.Status.Done) }
        )
    }

    fun byFlow(flow: Int): List<BoardScreenEntry> = all.filter { it.flow == flow }

    fun find(id: String): BoardScreenEntry? = all.find { it.id == id }
}
