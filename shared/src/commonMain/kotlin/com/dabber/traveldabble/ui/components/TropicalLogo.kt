package com.dabber.traveldabble.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.ui.theme.AuroraTeal

/**
 * Authentic Tropical "TD" Monogram Logo for Travel Dabble.
 * Rendered using 100% pure Compose Multiplatform vector drawing for high fidelity,
 * crisp scaling across any resolution, and zero runtime crashes.
 */
@Composable
fun TropicalLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showBackground: Boolean = true,
) {
    val cornerRadius = size * 0.26f

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            spotColor = AuroraTeal.copy(alpha = 0.40f),
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                } else {
                    Modifier
                }
            ),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleX = this.size.width / 100f
            val scaleY = this.size.height / 100f

            scale(scaleX, scaleY, pivot = Offset.Zero) {
                // 1. Badge Background
                if (showBackground) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF064E3B), // Deep Tropical Green
                                Color(0xFF047857), // Emerald
                                Color(0xFF0F172A), // Midnight Navy
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f),
                        ),
                        size = Size(100f, 100f),
                        cornerRadius = CornerRadius(26f, 26f),
                    )
                }

                // 2. Setting Sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A),
                            Color(0xFFF59E0B),
                            Color(0xFFD97706),
                        ),
                        center = Offset(52f, 32f),
                        radius = 10f,
                    ),
                    radius = 10f,
                    center = Offset(52f, 32f),
                )

                // 3. Sand Mound Base for Palm Tree
                val islandPath = Path().apply {
                    moveTo(16f, 76f)
                    cubicTo(22f, 73f, 36f, 73f, 44f, 76f)
                    cubicTo(36f, 77.5f, 22f, 77.5f, 16f, 76f)
                    close()
                }
                drawPath(islandPath, Color(0xFFF59E0B).copy(alpha = 0.65f))

                // 4. Letter "T" — Palm Tree Trunk
                val trunkPath = Path().apply {
                    moveTo(27.5f, 74f)
                    cubicTo(29.5f, 62f, 29f, 50f, 31.5f, 38f)
                    lineTo(35.5f, 38f)
                    cubicTo(33f, 50f, 33.5f, 62f, 31.5f, 74f)
                    close()
                }
                drawPath(trunkPath, Color.White)

                // 5. Letter "T" — Palm Tree Fronds (Crossbar)
                // Center Frond
                val centerFrond = Path().apply {
                    moveTo(33.5f, 39f)
                    cubicTo(32f, 30f, 35f, 22f, 33.5f, 18f)
                    cubicTo(36f, 23f, 37f, 31f, 33.5f, 39f)
                    close()
                }
                drawPath(centerFrond, Color(0xFF34D399))

                // Left Fronds
                val leftFrond1 = Path().apply {
                    moveTo(33.5f, 38f)
                    cubicTo(26f, 34f, 18f, 33f, 13f, 36f)
                    cubicTo(18f, 39f, 26f, 39f, 33.5f, 40f)
                    close()
                }
                drawPath(leftFrond1, Color(0xFF10B981))

                val leftFrond2 = Path().apply {
                    moveTo(33.5f, 38f)
                    cubicTo(24f, 30f, 17f, 28f, 14f, 31f)
                    cubicTo(18f, 34f, 25f, 34f, 33.5f, 39f)
                    close()
                }
                drawPath(leftFrond2, Color(0xFF34D399))

                val leftFrond3 = Path().apply {
                    moveTo(33.5f, 40f)
                    cubicTo(23f, 41f, 16f, 45f, 13f, 50f)
                    cubicTo(18f, 47f, 25f, 44f, 33.5f, 41f)
                    close()
                }
                drawPath(leftFrond3, Color(0xFF059669))

                // Right Fronds
                val rightFrond1 = Path().apply {
                    moveTo(33.5f, 38f)
                    cubicTo(41f, 34f, 47f, 33f, 51f, 36f)
                    cubicTo(47f, 39f, 40f, 39f, 33.5f, 40f)
                    close()
                }
                drawPath(rightFrond1, Color(0xFF10B981))

                val rightFrond2 = Path().apply {
                    moveTo(33.5f, 38f)
                    cubicTo(42f, 30f, 48f, 28f, 50f, 31f)
                    cubicTo(47f, 34f, 41f, 34f, 33.5f, 39f)
                    close()
                }
                drawPath(rightFrond2, Color(0xFF34D399))

                val rightFrond3 = Path().apply {
                    moveTo(33.5f, 40f)
                    cubicTo(42f, 42f, 47f, 46f, 49f, 51f)
                    cubicTo(45f, 47f, 40f, 44f, 33.5f, 41f)
                    close()
                }
                drawPath(rightFrond3, Color(0xFF059669))

                // Coconuts Cluster
                drawCircle(Color(0xFFF59E0B), radius = 1.8f, center = Offset(31.5f, 40f))
                drawCircle(Color(0xFFFBBF24), radius = 1.8f, center = Offset(35f, 40f))
                drawCircle(Color(0xFFD97706), radius = 1.6f, center = Offset(33.5f, 42f))

                // 6. Letter "D" — Sailboat Mast
                drawRect(
                    color = Color.White,
                    topLeft = Offset(54.5f, 28f),
                    size = Size(3f, 45f),
                )

                // Mast Wind Pennant
                val pennantPath = Path().apply {
                    moveTo(57.5f, 28f)
                    lineTo(64f, 30.5f)
                    lineTo(57.5f, 33f)
                    close()
                }
                drawPath(pennantPath, Color(0xFFF59E0B))

                // 7. Letter "D" — Main Sail (Outer Curved Loop)
                val mainSailPath = Path().apply {
                    moveTo(57.5f, 33f)
                    cubicTo(68f, 35f, 79f, 44f, 79f, 54.5f)
                    cubicTo(79f, 65f, 68f, 71f, 57.5f, 72f)
                    close()
                }
                drawPath(
                    path = mainSailPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
                        start = Offset(57.5f, 33f),
                        end = Offset(79f, 72f),
                    ),
                )

                // Inner Cutout Window of D
                val innerCutout = Path().apply {
                    moveTo(61f, 41f)
                    cubicTo(68f, 43f, 73f, 48f, 73f, 54.5f)
                    cubicTo(73f, 61f, 68f, 64.5f, 61f, 65.5f)
                    close()
                }
                drawPath(innerCutout, Color(0xFF047857))

                // Ocean Breeze Wave on Sail
                val breezePath = Path().apply {
                    moveTo(61f, 51f)
                    cubicTo(66f, 49f, 71f, 51f, 74f, 54f)
                    cubicTo(70f, 56f, 65f, 55f, 61f, 56f)
                    close()
                }
                drawPath(
                    path = breezePath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF38BDF8)),
                        start = Offset(61f, 51f),
                        end = Offset(74f, 56f),
                    ),
                )

                // 8. Boat Hull (Base of D)
                val hullPath = Path().apply {
                    moveTo(48f, 73f)
                    lineTo(78f, 73f)
                    cubicTo(79f, 73f, 80f, 74f, 79.5f, 75f)
                    cubicTo(77f, 78f, 69f, 80f, 56f, 80f)
                    cubicTo(47f, 80f, 47f, 75f, 48f, 73f)
                    close()
                }
                drawPath(hullPath, Color.White)

                // Hull Waterline Accent
                val waterlinePath = Path().apply {
                    moveTo(50f, 76.5f)
                    cubicTo(60f, 76.5f, 72f, 75.5f, 77f, 74.5f)
                }
                drawPath(
                    path = waterlinePath,
                    color = Color(0xFF06B6D4),
                    style = Stroke(width = 1.2f, cap = StrokeCap.Round),
                )

                // 9. Tropical Ocean Wave Ripples
                val wave1 = Path().apply {
                    moveTo(18f, 82f)
                    cubicTo(28f, 80f, 38f, 84f, 48f, 81.5f)
                    cubicTo(58f, 79f, 68f, 83f, 78f, 81f)
                    cubicTo(82f, 80f, 85f, 81.5f, 87f, 83f)
                }
                drawPath(
                    path = wave1,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF38BDF8)),
                        start = Offset(18f, 82f),
                        end = Offset(87f, 83f),
                    ),
                    style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                val wave2 = Path().apply {
                    moveTo(28f, 87f)
                    cubicTo(38f, 85.5f, 48f, 88f, 58f, 86.5f)
                    cubicTo(68f, 85f, 76f, 87.5f, 82f, 86.5f)
                }
                drawPath(
                    path = wave2,
                    color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                    style = Stroke(width = 1.2f, cap = StrokeCap.Round),
                )
            }
        }
    }
}
