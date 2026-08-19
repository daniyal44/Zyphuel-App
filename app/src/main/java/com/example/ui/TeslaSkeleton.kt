package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ZyphuelBlueDark
import com.example.ui.theme.ZyphuelBluePrimary

/**
 * Tesla-inspired subtle specular shimmer modifier.
 * Creates an ultra-smooth, high-tech metallic sweep across the component canvas.
 */
@Composable
fun Modifier.teslaShimmer(
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    isDark: Boolean = false
): Modifier {
    if (!enabled) return this

    val transition = rememberInfiniteTransition(label = "TeslaShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1350,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "TeslaShimmerTranslation"
    )

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF334155).copy(alpha = 0.85f),
            Color(0xFF00E5FF).copy(alpha = 0.12f),
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFEDF2F7),
            Color(0xFFFFFFFF).copy(alpha = 0.85f),
            Color(0xFF00A3FF).copy(alpha = 0.10f),
            Color(0xFFEDF2F7),
            Color(0xFFE2E8F0)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim * 0.4f),
        end = Offset(translateAnim + 400f, translateAnim * 0.4f + 300f)
    )

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(brush)
        .border(
            width = 0.75.dp,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFCBD5E1).copy(alpha = 0.5f),
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Reusable Tesla Skeleton element.
 */
@Composable
fun TeslaSkeletonItem(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isDark: Boolean = false,
    testTag: String = "tesla_skeleton_item"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .teslaShimmer(cornerRadius = cornerRadius, isDark = isDark)
    )
}

/**
 * Tesla-inspired Loading Skeleton Screen for Login / Registration.
 */
@Composable
fun TeslaAuthSkeleton(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tesla Shield / Logo Skeleton
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .teslaShimmer(cornerRadius = 34.dp, isDark = isDark),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF38BDF8).copy(alpha = 0.2f) else ZyphuelBluePrimary.copy(alpha = 0.15f))
                )
            }

            // Brand Title & Subtitle Skeletons
            TeslaSkeletonItem(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp),
                cornerRadius = 6.dp,
                isDark = isDark
            )
            TeslaSkeletonItem(
                modifier = Modifier
                    .width(240.dp)
                    .height(14.dp),
                cornerRadius = 4.dp,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Auth Form Card Skeleton
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131B2E) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card Title Skeleton
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(140.dp)
                            .height(18.dp),
                        cornerRadius = 6.dp,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Input Field 1 (Email)
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        cornerRadius = 14.dp,
                        isDark = isDark
                    )

                    // Input Field 2 (Password)
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        cornerRadius = 14.dp,
                        isDark = isDark
                    )

                    // Forgot Password Right-aligned Link Skeleton
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TeslaSkeletonItem(
                            modifier = Modifier
                                .width(110.dp)
                                .height(12.dp),
                            cornerRadius = 4.dp,
                            isDark = isDark
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main Action Button (Login)
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        cornerRadius = 14.dp,
                        isDark = isDark
                    )

                    // Divider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        )
                        TeslaSkeletonItem(
                            modifier = Modifier
                                .width(80.dp)
                                .height(10.dp),
                            cornerRadius = 3.dp,
                            isDark = isDark
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        )
                    }

                    // Social Continue with Google Button Skeleton
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        cornerRadius = 14.dp,
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Switch Link Skeleton
            TeslaSkeletonItem(
                modifier = Modifier
                    .width(220.dp)
                    .height(14.dp),
                cornerRadius = 4.dp,
                isDark = isDark
            )
        }
    }
}

/**
 * Tesla-inspired Loading Skeleton Screen for Customer & Rider Home Dashboard.
 */
@Composable
fun TeslaHomeSkeleton(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Top Bar Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TeslaSkeletonItem(
                    modifier = Modifier.size(36.dp),
                    cornerRadius = 18.dp,
                    isDark = isDark
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(90.dp)
                            .height(14.dp),
                        cornerRadius = 4.dp,
                        isDark = isDark
                    )
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(130.dp)
                            .height(10.dp),
                        cornerRadius = 3.dp,
                        isDark = isDark
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeslaSkeletonItem(
                    modifier = Modifier.size(36.dp),
                    cornerRadius = 18.dp,
                    isDark = isDark
                )
                TeslaSkeletonItem(
                    modifier = Modifier.size(36.dp),
                    cornerRadius = 18.dp,
                    isDark = isDark
                )
            }
        }

        // Live Location / GPS Banner Skeleton
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2F6)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeslaSkeletonItem(
                    modifier = Modifier.size(24.dp),
                    cornerRadius = 12.dp,
                    isDark = isDark
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp),
                        cornerRadius = 3.dp,
                        isDark = isDark
                    )
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(10.dp),
                        cornerRadius = 3.dp,
                        isDark = isDark
                    )
                }
                TeslaSkeletonItem(
                    modifier = Modifier
                        .width(60.dp)
                        .height(26.dp),
                    cornerRadius = 8.dp,
                    isDark = isDark
                )
            }
        }

        // Hero Order Now / Status Banner Skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF131B2E) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(140.dp)
                            .height(18.dp),
                        cornerRadius = 4.dp,
                        isDark = isDark
                    )
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(200.dp)
                            .height(12.dp),
                        cornerRadius = 3.dp,
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TeslaSkeletonItem(
                        modifier = Modifier
                            .width(100.dp)
                            .height(28.dp),
                        cornerRadius = 8.dp,
                        isDark = isDark
                    )
                }
                TeslaSkeletonItem(
                    modifier = Modifier.size(70.dp),
                    cornerRadius = 16.dp,
                    isDark = isDark
                )
            }
        }

        // Live Fuel Prices Section Header Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeslaSkeletonItem(
                modifier = Modifier
                    .width(130.dp)
                    .height(16.dp),
                cornerRadius = 4.dp,
                isDark = isDark
            )
            TeslaSkeletonItem(
                modifier = Modifier
                    .width(70.dp)
                    .height(12.dp),
                cornerRadius = 3.dp,
                isDark = isDark
            )
        }

        // Fuel Price Cards Grid Skeletons (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131B2E) else Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TeslaSkeletonItem(modifier = Modifier.size(24.dp), cornerRadius = 6.dp, isDark = isDark)
                        TeslaSkeletonItem(modifier = Modifier.width(36.dp).height(10.dp), cornerRadius = 3.dp, isDark = isDark)
                    }
                    TeslaSkeletonItem(modifier = Modifier.width(70.dp).height(18.dp), cornerRadius = 4.dp, isDark = isDark)
                }
            }

            // Card 2
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131B2E) else Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TeslaSkeletonItem(modifier = Modifier.size(24.dp), cornerRadius = 6.dp, isDark = isDark)
                        TeslaSkeletonItem(modifier = Modifier.width(36.dp).height(10.dp), cornerRadius = 3.dp, isDark = isDark)
                    }
                    TeslaSkeletonItem(modifier = Modifier.width(70.dp).height(18.dp), cornerRadius = 4.dp, isDark = isDark)
                }
            }
        }

        // Active Orders / Tracking Section Skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131B2E) else Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeslaSkeletonItem(modifier = Modifier.size(42.dp), cornerRadius = 12.dp, isDark = isDark)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TeslaSkeletonItem(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp), cornerRadius = 3.dp, isDark = isDark)
                    TeslaSkeletonItem(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp), cornerRadius = 3.dp, isDark = isDark)
                }
                TeslaSkeletonItem(modifier = Modifier.width(50.dp).height(24.dp), cornerRadius = 6.dp, isDark = isDark)
            }
        }
    }
}
