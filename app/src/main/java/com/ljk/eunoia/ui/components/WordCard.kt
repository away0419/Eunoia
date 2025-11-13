package com.ljk.eunoia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 단어 카드 컴포넌트 (토스 스타일 - 개선)
 */
@Composable
fun WordCard(
    word: WordData,
    showDate: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = {
                            isPressed = true
                            onClick()
                            // 터치 효과를 위해 잠시 후 원래 상태로
                            scope.launch {
                                delay(150)
                                isPressed = false
                            }
                        }
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 1.dp else 4.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 카테고리, 출처, 날짜
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 카테고리와 출처 배지
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 카테고리 배지
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = word.category,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    
                    // 출처 배지 (모든 단어에 표시)
                    val source = word.source ?: "asset" // null이면 기본값 사용
                    val (sourceText, sourceColor) = when (source) {
                        "ai" -> "🤖 AI" to androidx.compose.ui.graphics.Color(0xFF9C27B0) // 보라색
                        "user" -> "✏️ 직접 추가" to androidx.compose.ui.graphics.Color(0xFF4CAF50) // 초록색
                        "asset" -> "📚 기본 단어" to androidx.compose.ui.graphics.Color(0xFF2196F3) // 파란색
                        else -> "📚 기본 단어" to androidx.compose.ui.graphics.Color(0xFF2196F3) // 기본값도 파란색
                    }
                    
                    Surface(
                        color = sourceColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = sourceText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = sourceColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
                
                if (showDate && word.date != null) {
                    Text(
                        text = word.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            
            // 단어 (더 강조)
            Text(
                text = word.word,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp
            )
            
            // 구분선
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Divider.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            
            // 뜻 (더 읽기 쉽게)
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

