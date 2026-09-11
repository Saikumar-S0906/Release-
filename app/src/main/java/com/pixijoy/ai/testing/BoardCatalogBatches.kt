package com.pixijoy.ai.testing

import com.pixijoy.ai.ui.board.BoardUiCatalog

/**
 * Batched board catalog slices for instrumented Compose runs.
 * Avoids melting an AVD with a single ~300+ parameterized class.
 *
 * Nightly / full: run every batch (or legacy [com.pixijoy.ai.board.BoardCatalogComposeTest]).
 * Daily: run batches one at a time via `scripts/run_board_qa_depth.ps1`.
 */
object BoardCatalogBatches {

    data class Batch(
        val id: String,
        val name: String,
        /** Inclusive catalog filter. */
        val matches: (String) -> Boolean
    )

    val all: List<Batch> = listOf(
        Batch("core", "Onboarding · Home · Input · Understanding") { id ->
            id.startsWith("b1_") || id.startsWith("b2_") || id.startsWith("b3_") ||
                id.startsWith("b4_") || id == "b_create_hub" || id.startsWith("b_c00_")
        },
        Batch("create", "Rematch Beautiful / Funny / Tell / Different · F19") { id ->
            id.startsWith("b5_") || id.startsWith("b6_") || id.startsWith("b7_") ||
                id.startsWith("b_diff") || id.startsWith("b_different") || id.startsWith("b19_")
        },
        Batch("ctools", "C04–C14 tool wizards") { id ->
            !id.startsWith("b_c00") && (id.startsWith("b_c0") || id.startsWith("b_c1"))
        },
        Batch("media", "Stickers · Move · Editor · Collage · Export") { id ->
            id.startsWith("b13_") || id.startsWith("b14_") || id.startsWith("b15_") ||
                id.startsWith("b16_") || id.startsWith("b17_")
        },
        Batch("library", "Save/Share · Creations · Search · Collab") { id ->
            id.startsWith("b8_") || id.startsWith("b18_") || id.startsWith("b26_") ||
                id.startsWith("b_collab_")
        },
        Batch("me", "Profile · Pro · Settings · Safety · Privacy · Auth · Credits · Sub · Notifs") { id ->
            id.startsWith("b9_") || id.startsWith("b10_") || id.startsWith("b11_") ||
                id.startsWith("b20_") || id.startsWith("b21_") || id.startsWith("b22_") ||
                id.startsWith("b23_") || id.startsWith("b24_") || id.startsWith("b25_")
        },
        Batch("social", "Journey · Community · Offline · A11y · Lifecycle") { id ->
            id.startsWith("b12_") || id.startsWith("b27_") || id.startsWith("b28_") ||
                id.startsWith("b29_") || id.startsWith("b30_")
        }
    )

    fun byId(id: String): Batch = all.first { it.id == id }

    fun screenIds(batchId: String): List<String> {
        val batch = byId(batchId)
        return BoardUiCatalog.all.map { it.id }.filter(batch.matches)
    }

    /** Every catalog id must land in exactly one batch. */
    fun uncoveredIds(): List<String> {
        val covered = all.flatMap { batch -> BoardUiCatalog.all.map { it.id }.filter(batch.matches) }.toSet()
        return BoardUiCatalog.all.map { it.id }.filter { it !in covered }.sorted()
    }

    fun overlappingIds(): List<String> {
        val counts = mutableMapOf<String, Int>()
        all.forEach { batch ->
            BoardUiCatalog.all.map { it.id }.filter(batch.matches).forEach { id ->
                counts[id] = (counts[id] ?: 0) + 1
            }
        }
        return counts.filter { it.value > 1 }.keys.sorted()
    }

    /**
     * Key rematch / create entry screens for phone golden capture
     * (side-by-side vs `flows/additional/REMATCH_*`).
     */
    val goldenKeyScreenIds: List<String> = listOf(
        "b2_home",
        "b_create_hub",
        "b5_choose",
        "b6_choose",
        "b7_sheet",
        "b_diff_choose",
        "b5_result",
        "b8_ready",
        "b8_share",
        "b18_browse",
        "b13_photo",
        "b14_input",
        "b_c04",
        "b_c07"
    )

    /**
     * Scripted day-in-life path: Home → Create → Beautiful rematch → Share → My Stuff.
     * Soft AI only — host navigation, no live backends.
     */
    val dayInLifeSteps: List<String> = listOf(
        "b2_home",
        "b_create_hub",
        "b5_choose",
        "b5_adjust",
        "b5_processing",
        "b5_result",
        "b8_ready",
        "b8_share",
        "b18_browse"
    )
}
