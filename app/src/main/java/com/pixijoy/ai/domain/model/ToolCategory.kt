package com.pixijoy.ai.domain.model

import androidx.annotation.StringRes
import com.pixijoy.ai.R

enum class ToolCategory(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int
) {
    VIRAL_CREATE(R.string.category_viral_create, R.string.category_viral_create_desc),
    AI_STUDIO(R.string.category_ai_studio, R.string.category_ai_studio_desc),
    SOCIAL_EXPORT(R.string.category_social_export, R.string.category_social_export_desc)
}
