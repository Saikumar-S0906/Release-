package com.pixijoy.ai.testing

/**
 * Canonical soft-AI board journeys for automated UI / functional coverage.
 * Screen ids must exist in [com.pixijoy.ai.ui.board.BoardUiCatalog].
 */
object BoardFlowJourneys {

    data class Journey(
        val id: String,
        val name: String,
        val entry: String,
        val steps: List<String>,
        val hostTag: String? = null,
        val edgeIds: List<String> = emptyList()
    )

    val all: List<Journey> = listOf(
        Journey(
            id = "F01",
            name = "Launch & Onboarding",
            entry = "b1_splash",
            steps = listOf(
                "b1_splash", "b1_welcome", "b1_features", "b1_for_everyone",
                "b1_permissions", "b1_signup", "b1_ready"
            ),
            edgeIds = listOf(
                "b1_skip", "b1_perm_denied", "b1_limited",
                "b1_signup_later", "b1_returning", "b1_whats_new"
            )
        ),
        Journey(
            id = "F02",
            name = "Home & Discover",
            entry = "b2_home",
            steps = listOf(
                "b2_home", "b2_discover", "b2_template", "b2_search",
                "b2_trending", "b2_collections", "b2_favorites", "b2_recent"
            ),
            hostTag = TestTags.FLOW2_HOME
        ),
        Journey(
            id = "C00",
            name = "Empty Home First Creation",
            entry = "b_c00_empty",
            steps = listOf(
                "b_c00_empty", "b_c00_taste", "b_c00_discover", "b_c00_funny",
                "b_c00_generating", "b_c00_first"
            ),
            hostTag = TestTags.FLOW_C00_HOST,
            edgeIds = listOf(
                "b_c00_guest_gate", "b_c00_perm_denied", "b_c00_guest",
                "b_c00_skip_taste", "b_c00_sample"
            )
        ),
        Journey(
            id = "F03",
            name = "Photo / Video Input",
            entry = "b3_open",
            steps = listOf("b3_open", "b3_chooser", "b3_preview", "b3_multi", "b3_proceed"),
            hostTag = TestTags.FLOW3_CHOOSER,
            edgeIds = listOf(
                "b3_camera_denied", "b3_photos_denied", "b3_limited",
                "b3_too_large", "b3_unsupported", "b3_no_photos", "b3_corrupt"
            )
        ),
        Journey(
            id = "F04",
            name = "AI Understanding",
            entry = "b4_selected",
            steps = listOf(
                "b4_selected", "b4_analyzing", "b4_understanding",
                "b4_suggestions", "b4_choose_path", "b4_start"
            ),
            hostTag = TestTags.FLOW4_RESULT,
            edgeIds = listOf(
                "b4_offline", "b4_safety", "b4_no_subject",
                "b4_multi_subject", "b4_low_quality", "b4_no_face", "b4_unsupported"
            )
        ),
        Journey(
            id = "F05",
            name = "Make it Beautiful",
            entry = "b5_choose",
            steps = listOf(
                "b5_choose", "b5_adjust", "b5_processing", "b5_result",
                "b5_variations", "b5_edit", "b5_ready"
            ),
            hostTag = TestTags.FLOW5_BEAUTIFUL,
            edgeIds = listOf("b5_offline", "b5_low_credits", "b5_safety")
        ),
        Journey(
            id = "F06",
            name = "Make it Funny",
            entry = "b6_choose",
            steps = listOf(
                "b6_choose", "b6_customize", "b6_processing", "b6_result",
                "b6_variations", "b6_share"
            ),
            hostTag = TestTags.FLOW6_FUNNY,
            edgeIds = listOf("b6_offline", "b6_low_credits", "b6_safety")
        ),
        Journey(
            id = "F07",
            name = "Tell PixiJoy",
            entry = "b7_sheet",
            steps = listOf(
                "b7_sheet", "b7_input", "b7_request", "b7_understanding",
                "b7_generating", "b7_result", "b7_next"
            ),
            hostTag = TestTags.FLOW7_TELL,
            edgeIds = listOf("b7_offline", "b7_low_credits", "b7_safety")
        ),
        Journey(
            id = "DIFF",
            name = "Make it Different",
            entry = "b_diff_choose",
            steps = listOf(
                "b_diff_choose", "b_diff_options", "b_diff_adjust", "b_diff_processing",
                "b_diff_result", "b_diff_variations", "b_diff_share"
            ),
            hostTag = TestTags.FLOW_DIFF_HOST,
            edgeIds = listOf("b_diff_offline", "b_diff_low_credits", "b_diff_safety")
        ),
        Journey(
            id = "F19",
            name = "AI Generation Recovery",
            entry = "b19_start",
            steps = listOf(
                "b19_start", "b19_analyzing", "b19_generating", "b19_almost", "b19_success"
            ),
            hostTag = TestTags.FLOW19_START,
            edgeIds = listOf(
                "b19_canceled", "b19_offline", "b19_busy", "b19_timeout",
                "b19_unsupported", "b19_safety", "b19_low_credits", "b19_failed"
            )
        ),
        Journey(
            id = "C04",
            name = "Remove Background",
            entry = "b_c04",
            steps = listOf(
                "b_c04", "b_c04_photo", "b_c04_options", "b_c04_processing",
                "b_c04_refine", "b_c04_variations", "b_c04_result", "b_c04_erase", "b_c04_share"
            ),
            hostTag = TestTags.FLOW_C04_HOST,
            edgeIds = listOf("b_c04_offline", "b_c04_low_credits", "b_c04_safety")
        ),
        Journey(
            id = "C05",
            name = "Restore Old Photo",
            entry = "b_c05",
            steps = listOf(
                "b_c05", "b_c05_photo", "b_c05_options", "b_c05_processing",
                "b_c05_result", "b_c05_variations", "b_c05_edit", "b_c05_share"
            ),
            hostTag = TestTags.FLOW_C05_HOST,
            edgeIds = listOf("b_c05_offline", "b_c05_low_credits", "b_c05_safety")
        ),
        Journey(
            id = "C06",
            name = "Face Swap",
            entry = "b_c06",
            steps = listOf(
                "b_c06", "b_c06_photo", "b_c06_consent", "b_c06_options",
                "b_c06_processing", "b_c06_result", "b_c06_variations", "b_c06_share"
            ),
            hostTag = TestTags.FLOW_C06_HOST,
            edgeIds = listOf("b_c06_consent_denied", "b_c06_offline", "b_c06_low_credits", "b_c06_safety")
        ),
        Journey(
            id = "C07",
            name = "Change Scene",
            entry = "b_c07",
            steps = listOf(
                "b_c07", "b_c07_photo", "b_c07_options", "b_c07_adjust",
                "b_c07_processing", "b_c07_result", "b_c07_variations", "b_c07_share"
            ),
            hostTag = TestTags.FLOW_C07_HOST,
            edgeIds = listOf("b_c07_offline", "b_c07_low_credits", "b_c07_safety")
        ),
        Journey(
            id = "C08",
            name = "Add or Remove People",
            entry = "b_c08",
            steps = listOf(
                "b_c08", "b_c08_photo", "b_c08_options", "b_c08_adjust", "b_c08_consent",
                "b_c08_processing", "b_c08_result", "b_c08_variations", "b_c08_share"
            ),
            hostTag = TestTags.FLOW_C08_HOST,
            edgeIds = listOf("b_c08_consent_denied", "b_c08_offline", "b_c08_low_credits", "b_c08_safety")
        ),
        Journey(
            id = "C09",
            name = "AI Portrait",
            entry = "b_c09",
            steps = listOf(
                "b_c09", "b_c09_photo", "b_c09_options", "b_c09_processing",
                "b_c09_result", "b_c09_variations", "b_c09_share"
            ),
            hostTag = TestTags.FLOW_C09_HOST,
            edgeIds = listOf("b_c09_offline", "b_c09_low_credits", "b_c09_safety")
        ),
        Journey(
            id = "C10",
            name = "Expand",
            entry = "b_c10",
            steps = listOf(
                "b_c10", "b_c10_photo", "b_c10_options", "b_c10_filters",
                "b_c10_processing", "b_c10_erase", "b_c10_result", "b_c10_variations", "b_c10_share"
            ),
            hostTag = TestTags.FLOW_C10_HOST,
            edgeIds = listOf("b_c10_offline", "b_c10_low_credits", "b_c10_safety")
        ),
        Journey(
            id = "C11",
            name = "Talking Photo",
            entry = "b_c11",
            steps = listOf(
                "b_c11", "b_c11_photo", "b_c11_options", "b_c11_adjust",
                "b_c11_processing", "b_c11_result", "b_c11_share", "b_c11_variations"
            ),
            hostTag = TestTags.FLOW_C11_HOST,
            edgeIds = listOf("b_c11_offline", "b_c11_low_credits", "b_c11_safety")
        ),
        Journey(
            id = "C12",
            name = "Greeting Card",
            entry = "b_c12",
            steps = listOf(
                "b_c12", "b_c12_photo", "b_c12_options", "b_c12_edit",
                "b_c12_processing", "b_c12_result", "b_c12_variations", "b_c12_share"
            ),
            hostTag = TestTags.FLOW_C12_HOST,
            edgeIds = listOf("b_c12_offline", "b_c12_low_credits", "b_c12_safety")
        ),
        Journey(
            id = "C14",
            name = "Remix from Community",
            entry = "b_c14",
            steps = listOf(
                "b_c14", "b_c14_photo", "b_c14_options", "b_c14_processing",
                "b_c14_result", "b_c14_variations", "b_c14_share"
            ),
            hostTag = TestTags.FLOW_C14_HOST,
            edgeIds = listOf("b_c14_offline", "b_c14_low_credits", "b_c14_safety")
        ),
        Journey(
            id = "F13",
            name = "Stickers",
            entry = "b13_photo",
            steps = listOf(
                "b13_photo", "b13_cutout", "b13_refine", "b13_express",
                "b13_pack", "b13_preview", "b13_share"
            ),
            hostTag = TestTags.FLOW13_HOST,
            edgeIds = listOf("b13_offline", "b13_low_credits", "b13_safety")
        ),
        Journey(
            id = "F14",
            name = "Make it Move",
            entry = "b14_input",
            steps = listOf(
                "b14_input", "b14_media", "b14_motion", "b14_customize",
                "b14_gen", "b14_preview", "b14_save", "b14_stories"
            ),
            hostTag = TestTags.FLOW14_HOST,
            edgeIds = listOf("b14_offline", "b14_low_credits", "b14_safety")
        ),
        Journey(
            id = "F15",
            name = "Advanced Photo Editor",
            entry = "b15_open",
            steps = listOf(
                "b15_open", "b15_workspace", "b15_crop", "b15_adjust",
                "b15_filters", "b15_text", "b15_more", "b15_save"
            ),
            hostTag = TestTags.FLOW15_HOST,
            edgeIds = listOf("b15_offline", "b15_low_credits", "b15_safety")
        ),
        Journey(
            id = "F16",
            name = "Multi Photo Collage",
            entry = "b16_select",
            steps = listOf(
                "b16_select", "b16_purpose", "b16_arrange", "b16_creating",
                "b16_preview", "b16_save", "b16_end"
            ),
            hostTag = TestTags.FLOW16_HOST,
            edgeIds = listOf("b16_offline", "b16_low_credits", "b16_safety")
        ),
        Journey(
            id = "F17",
            name = "Export Platform Optimization",
            entry = "b17_open",
            steps = listOf(
                "b17_open", "b17_platform", "b17_optimize", "b17_preview",
                "b17_advanced", "b17_staging", "b17_exporting", "b17_done"
            ),
            hostTag = TestTags.FLOW17_HOST,
            edgeIds = listOf("b17_offline", "b17_low_credits", "b17_safety")
        ),
        Journey(
            id = "F08",
            name = "Save Share Community",
            entry = "b8_ready",
            steps = listOf(
                "b8_ready", "b8_preview", "b8_saved", "b8_share",
                "b8_publish", "b8_live", "b8_manage"
            ),
            hostTag = TestTags.FLOW8_HOST
        ),
        Journey(
            id = "COLLAB",
            name = "Sharing & Collaboration",
            entry = "b_collab_select",
            steps = listOf(
                "b_collab_select", "b_collab_share", "b_collab_people",
                "b_collab_message", "b_collab_perms", "b_collab_ready",
                "b_collab_album", "b_collab_activity", "b_collab_together"
            ),
            hostTag = TestTags.FLOW_COLLAB_HOST
        ),
        Journey(
            id = "F18",
            name = "My Creations",
            entry = "b18_browse",
            steps = listOf(
                "b18_browse", "b18_detail", "b18_search", "b18_album",
                "b18_versions", "b18_trash", "b18_more"
            ),
            hostTag = TestTags.FLOW18_HOST
        ),
        Journey(
            id = "F26",
            name = "Search Templates",
            entry = "b26_open",
            steps = listOf(
                "b26_open", "b26_search", "b26_results", "b26_detail",
                "b26_customize", "b26_filters", "b26_saved", "b26_more"
            ),
            hostTag = TestTags.FLOW26_HOST
        ),
        Journey(
            id = "F09",
            name = "Profile",
            entry = "b9_open",
            steps = listOf(
                "b9_open", "b9_edit", "b9_theme", "b9_albums",
                "b9_favorites", "b9_notifs", "b9_privacy", "b9_updated"
            ),
            hostTag = TestTags.FLOW9_HOST
        ),
        Journey(
            id = "F10",
            name = "Pro Subscription",
            entry = "b10_discover",
            steps = listOf(
                "b10_discover", "b10_welcome", "b10_benefits", "b10_compare",
                "b10_pay", "b10_confirm", "b10_manage", "b10_inapp"
            ),
            hostTag = TestTags.FLOW10_HOST
        ),
        Journey(
            id = "F11",
            name = "Settings Help",
            entry = "b11_hub",
            steps = listOf(
                "b11_hub", "b11_home", "b11_help", "b11_account",
                "b11_appearance", "b11_language", "b11_notifs", "b11_storage"
            ),
            hostTag = TestTags.FLOW11_HOST
        ),
        Journey(
            id = "F20",
            name = "AI Safety",
            entry = "b20_input",
            steps = listOf(
                "b20_input", "b20_check", "b20_result", "b20_blocked",
                "b20_allowed", "b20_modify", "b20_report", "b20_generating"
            ),
            hostTag = TestTags.FLOW20_HOST
        ),
        Journey(
            id = "F21",
            name = "Privacy Center",
            entry = "b21_hub",
            steps = listOf(
                "b21_hub", "b21_data", "b21_photos", "b21_personalization",
                "b21_download", "b21_confirm", "b21_done"
            ),
            hostTag = TestTags.FLOW21_HOST
        ),
        Journey(
            id = "F22",
            name = "Account Auth",
            entry = "b22_welcome",
            steps = listOf(
                "b22_welcome", "b22_methods", "b22_oauth", "b22_profile",
                "b22_perms", "b22_devices", "b22_manage", "b22_ready"
            ),
            hostTag = TestTags.FLOW22_HOST
        ),
        Journey(
            id = "F23",
            name = "AI Credits",
            entry = "b23_hub",
            steps = listOf(
                "b23_hub", "b23_dash", "b23_costs", "b23_packs",
                "b23_pay", "b23_added", "b23_use", "b23_history"
            ),
            hostTag = TestTags.FLOW23_HOST
        ),
        Journey(
            id = "F24",
            name = "Subscription Management",
            entry = "b24_plans",
            steps = listOf(
                "b24_plans", "b24_choose", "b24_matrix", "b24_pay",
                "b24_manage", "b24_billing", "b24_cancel", "b24_welcome"
            ),
            hostTag = TestTags.FLOW24_HOST
        ),
        Journey(
            id = "F25",
            name = "Notification Center",
            entry = "b25_hub",
            steps = listOf(
                "b25_hub", "b25_center", "b25_detail", "b25_empty",
                "b25_filtered", "b25_manage", "b25_push", "b25_settings"
            ),
            hostTag = TestTags.FLOW25_HOST
        ),
        Journey(
            id = "F12",
            name = "End to End Journey",
            entry = "b12_intro",
            steps = listOf(
                "b12_intro", "b12_welcome", "b12_home", "b12_create",
                "b12_edit", "b12_share", "b12_community", "b12_pro", "b12_signin"
            ),
            hostTag = TestTags.FLOW12_HOST
        ),
        Journey(
            id = "F27",
            name = "Community UGC",
            entry = "b27_feed",
            steps = listOf(
                "b27_feed", "b27_post", "b27_trending", "b27_categories",
                "b27_creators", "b27_creator", "b27_people", "b27_share", "b27_guidelines"
            ),
            hostTag = TestTags.FLOW27_HOST
        ),
        Journey(
            id = "F28",
            name = "Offline Network Recovery",
            entry = "b28_banner",
            steps = listOf(
                "b28_banner", "b28_modal", "b28_edit", "b28_saved",
                "b28_syncing", "b28_synced", "b28_failed", "b28_online"
            ),
            hostTag = TestTags.FLOW28_HOST
        ),
        Journey(
            id = "F29",
            name = "Accessibility Localization",
            entry = "b29_home",
            steps = listOf(
                "b29_home", "b29_a11y", "b29_appearance", "b29_language",
                "b29_localized", "b29_settings", "b29_voice", "b29_save"
            ),
            hostTag = TestTags.FLOW29_HOST
        ),
        Journey(
            id = "F30",
            name = "App Lifecycle Recovery",
            entry = "b30_home",
            steps = listOf(
                "b30_home", "b30_welcome", "b30_crash", "b30_restart",
                "b30_bg", "b30_device", "b30_update", "b30_integrity"
            ),
            hostTag = TestTags.FLOW30_HOST
        ),
        Journey(
            id = "DAY",
            name = "Day in life · Home → Create → Beautiful → Share → My Stuff",
            entry = "b2_home",
            steps = BoardCatalogBatches.dayInLifeSteps,
            hostTag = TestTags.FLOW2_HOME
        ),
        Journey(
            id = "CREATE",
            name = "Create Hub",
            entry = "b_create_hub",
            steps = listOf("b_create_hub")
        )
    )

    fun byId(id: String): Journey = all.first { it.id == id }

    fun allReferencedIds(): Set<String> =
        all.flatMap { it.steps + it.edgeIds + listOf(it.entry) }.toSet()
}
