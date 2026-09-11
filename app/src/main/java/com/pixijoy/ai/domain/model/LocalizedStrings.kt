package com.pixijoy.ai.domain.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.domain.model.ProPlanUi
import com.pixijoy.ai.R

@Composable
@ReadOnlyComposable
fun PhotoTool.localizedTitle(): String = stringResource(titleRes)

@Composable
@ReadOnlyComposable
fun PhotoTool.localizedDescription(): String = stringResource(descriptionRes)

fun PhotoTool.localizedTitle(context: Context): String = context.getString(titleRes)

fun PhotoTool.localizedDescription(context: Context): String = context.getString(descriptionRes)

@Composable
@ReadOnlyComposable
fun ToolCategory.localizedTitle(): String = stringResource(titleRes)

@Composable
@ReadOnlyComposable
fun ToolCategory.localizedSubtitle(): String = stringResource(subtitleRes)

@Composable
@ReadOnlyComposable
fun CreationType.localizedTitle(): String = stringResource(titleRes)

@Composable
@ReadOnlyComposable
fun CreationType.localizedDescription(): String = stringResource(descriptionRes)

fun CreationType.localizedTitle(context: Context): String = context.getString(titleRes)

@Composable
@ReadOnlyComposable
fun CaptionTone.localizedLabel(): String = stringResource(labelRes)

fun CaptionTone.promptLabel(context: Context): String = context.getString(labelRes)

@Composable
@ReadOnlyComposable
fun PhotoFilter.localizedLabel(): String = stringResource(labelRes)

@Composable
@ReadOnlyComposable
fun PaymentMethod.localizedLabel(): String = stringResource(labelRes)

@Composable
@ReadOnlyComposable
fun PaymentMethod.localizedDescription(): String = stringResource(descriptionRes)

@Composable
@ReadOnlyComposable
fun StickerEmotion.localizedLabel(): String = stringResource(labelRes)

fun StickerEmotion.localizedLabel(context: Context): String = context.getString(labelRes)

@Composable
@ReadOnlyComposable
fun SocialFormat.localizedLabel(): String = stringResource(labelRes)

fun SocialFormat.localizedLabel(context: Context): String = context.getString(labelRes)

@Composable
@ReadOnlyComposable
fun ProPlanUi.localizedPlan(): String = stringResource(planRes)

@Composable
@ReadOnlyComposable
fun ProPlanUi.localizedSubtitle(): String = stringResource(subtitleRes)

@Composable
@ReadOnlyComposable
fun ProPlanUi.localizedBadge(): String? = badgeRes?.let { stringResource(it) }

@Composable
@ReadOnlyComposable
fun stringRes(@StringRes resId: Int, vararg args: Any): String = stringResource(resId, *args)
