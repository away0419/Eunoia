package com.ljk.eunoia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.theme.*

/**
 * 퀴즈용 단어 카드 - 단어 또는 뜻 중 하나만 표시 (토스 스타일)
 */
@Composable
fun QuizWordCard(
    word: WordData,
    onCorrect: ((WordData) -> Unit)? = null,
    onIncorrect: ((WordData) -> Unit)? = null
) {
    // 랜덤하게 단어 또는 뜻 중 하나를 선택 (각 카드마다 고정)
    val showWordFirst = remember(word.id) { kotlin.random.Random.nextBoolean() }
    
    var isWordRevealed by remember { mutableStateOf(showWordFirst) }
    var isMeaningRevealed by remember { mutableStateOf(!showWordFirst) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 카테고리 배지
                Surface(
                    color = PrimaryBlueLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = word.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            color = AccentRed.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "X",
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    if (onCorrect != null) {
                        Surface(
                            onClick = { onCorrect(word) },
                            shape = CircleShape,
                            color = AccentGreen.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "O",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // 단어 영역
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (!isWordRevealed) {
                            isWordRevealed = true
                        }
                    },
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
                        text = "단어",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (isWordRevealed) {
                        Text(
                            text = word.word,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp
                        )
                    } else {
                        Text(
                            text = "???",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
            
            // 구분선
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Divider.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            
            // 뜻 영역
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (!isMeaningRevealed) {
                            isMeaningRevealed = true
                        }
                    },
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
                        text = "뜻",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (isMeaningRevealed) {
                        Text(
                            text = word.meaning,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    } else {
                        Text(
                            text = "???",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

