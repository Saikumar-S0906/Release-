package com.pixijoy.ai.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    before: Bitmap,
    after: Bitmap,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        var sliderPosition by remember { mutableFloatStateOf(0.5f) }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = after.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_after),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((maxWidthPx * sliderPosition).dp)
                    .clipToBounds()
            ) {
                Image(
                    bitmap = before.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_before),
                    modifier = Modifier
                        .width(maxWidthPx.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .offset { IntOffset((maxWidthPx * sliderPosition - 1.5f).roundToInt(), 0) }
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset((maxWidthPx * sliderPosition - 20.dp.toPx()).roundToInt(), 0) }
                    .align(Alignment.CenterStart)
                    .width(40.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            sliderPosition = (sliderPosition + dragAmount.x / maxWidthPx).coerceIn(0.05f, 0.95f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight(0.12f)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_compare),
                        contentDescription = stringResource(R.string.cd_compare),
                        tint = Color.Black,
                        modifier = Modifier.width(24.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.before),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = stringResource(R.string.after),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
