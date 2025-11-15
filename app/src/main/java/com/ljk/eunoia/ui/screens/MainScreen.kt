package com.ljk.eunoia.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import com.ljk.eunoia.ui.screens.tabs.HistoryTab
import com.ljk.eunoia.ui.screens.tabs.QuizTab
import com.ljk.eunoia.ui.screens.tabs.TodayTab
import com.ljk.eunoia.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 메인 화면 - 탭과 페이지 구성 (토스 스타일)
 */
@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("오늘의 단어", "지난 단어", "퀴즈")
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // 커스텀 탭 바 (토스 스타일)
            CustomTabBar(
                tabs = tabs,
                pagerState = pagerState,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onSettingsClick = { showSettings = true }
            )
            
            // 페이지 뷰
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TodayTab()
                    1 -> HistoryTab()
                    2 -> QuizTab()
                }
            }
        }
    }
}

/**
 * 커스텀 탭 바 (토스 스타일)
 */
@Composable
fun CustomTabBar(
    tabs: List<String>,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shadowElevation = 2.dp
    ) {
        Column {
            // 탭 버튼들과 설정 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 탭 영역 (설정 버튼 제외한 나머지 공간)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            CustomTab(
                                title = title,
                                selected = pagerState.currentPage == index,
                                onClick = { onTabSelected(index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // 설정 버튼 (고정 크기)
                TossIconButton(
                    onClick = onSettingsClick,
                    icon = Icons.Default.Settings,
                    contentDescription = "설정"
                )
            }
            
            // 선택된 탭 인디케이터 (탭 영역만 계산)
            TabIndicator(
                pagerState = pagerState,
                tabCount = tabs.size,
                hasSettingsButton = true
            )
        }
    }
}

/**
 * 커스텀 탭 버튼 (토스 스타일)
 */
@Composable
fun CustomTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (selected) TextPrimary else TextSecondary
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * 탭 인디케이터 (애니메이션) - 탭 영역만 계산
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TabIndicator(
    pagerState: PagerState,
    tabCount: Int,
    hasSettingsButton: Boolean = false
) {
    val indicatorWidth = 1f / tabCount
    val currentPage = pagerState.currentPage
    val pageOffset = pagerState.currentPageOffsetFraction
    
    // 인디케이터의 위치 계산 (0.0 ~ 1.0 범위)
    val indicatorPosition by animateFloatAsState(
        targetValue = (currentPage + pageOffset) * indicatorWidth,
        animationSpec = tween(durationMillis = 300),
        label = "indicator_position"
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        val totalWidth = maxWidth
        // 설정 버튼이 있으면 탭 영역만 계산 (설정 버튼 크기: 48.dp)
        val tabAreaWidth = if (hasSettingsButton) {
            totalWidth - 48.dp
        } else {
            totalWidth
        }
        val indicatorWidthDp = tabAreaWidth * indicatorWidth
        val offsetXDp = tabAreaWidth * indicatorPosition
        
        // 배경 바 (탭 영역만)
        Box(
            modifier = Modifier
                .width(tabAreaWidth)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Divider)
        )
        
        // 선택된 탭 인디케이터
        Box(
            modifier = Modifier
                .width(indicatorWidthDp)
                .height(3.dp)
                .offset(x = offsetXDp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(PrimaryBlue)
        )
    }
}

/**
 * 토스 스타일 아이콘 버튼
 */
@Composable
fun TossIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .padding(8.dp),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

