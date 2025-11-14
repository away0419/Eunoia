package com.ljk.eunoia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.theme.*

/**
 * 퀴즈용 단어 카드 - 블러 처리 기능 (토스 스타일)
 */
@Composable
fun QuizWordCard(
    word: WordData,
    onCorrect: ((WordData) -> Unit)? = null,
    onIncorrect: ((WordData) -> Unit)? = null
) {
    var isWordRevealed by remember { mutableStateOf(false) }
    var isMeaningRevealed by remember { mutableStateOf(false) }
    
    val wordAlpha by animateFloatAsState(
        targetValue = if (isWordRevealed) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "word_alpha"
    )
    
    val meaningAlpha by animateFloatAsState(
        targetValue = if (isMeaningRevealed) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "meaning_alpha"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onIncorrect != null) {
                        Surface(
                            onClick = { onIncorrect(word) },
                            shape = CircleShape,
                            color = Color(0xFFF44336).copy(alpha = 0.08f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "X",
                                    color = Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    if (onCorrect != null) {
                        Surface(
                            onClick = { onCorrect(word) },
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "O",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // 단어 (블러 처리 가능)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isWordRevealed = !isWordRevealed },
                color = BackgroundLight,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isWordRevealed) "단어" else "???",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = word.word,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(wordAlpha),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 32.sp
                    )
                }
            }
            
            // 구분선
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Divider.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            
            // 뜻 (블러 처리 가능)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMeaningRevealed = !isMeaningRevealed },
                color = BackgroundLight,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isMeaningRevealed) "뜻" else "???",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = word.meaning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(meaningAlpha),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

