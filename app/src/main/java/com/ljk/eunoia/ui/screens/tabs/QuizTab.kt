package com.ljk.eunoia.ui.screens.tabs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.components.QuizWordCard
import com.ljk.eunoia.ui.screens.TossIconButton
import com.ljk.eunoia.ui.theme.*
import com.ljk.eunoia.utils.FileManager
import kotlinx.coroutines.launch

/**
 * 퀴즈 탭 - 단어나 뜻을 블러 처리하고 탭하여 확인 (토스 스타일)
 */
@Composable
fun QuizTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var words by remember { mutableStateOf<List<WordData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 단어 로드 함수
    suspend fun loadWords() {
        try {
            words = FileManager.getQuizWords(context)
        } catch (e: Exception) {
            e.printStackTrace()
            words = emptyList()
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }
    
    // 초기 로드
    LaunchedEffect(Unit) {
        loadWords()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // 헤더
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBackground,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "퀴즈",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "단어나 뜻을 탭하여 정답을 확인하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                
                // 새로고침 버튼 (토스 스타일)
                if (isRefreshing) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryBlue,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    TossIconButton(
                        onClick = {
                            isRefreshing = true
                            scope.launch {
                                loadWords()
                            }
                        },
                        icon = Icons.Default.Refresh,
                        contentDescription = "새로고침"
                    )
                }
            }
        }
        
        // 단어 목록
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryBlue
                )
            } else if (words.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "표시할 단어가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    items(
                        items = words,
                        key = { it.id }
                    ) { word ->
                        QuizWordCard(
                            word = word,
                            onCorrect = { correctWord ->
                                // 맞춘 경우 저장하고 화면에서 제거
                                FileManager.saveQuizResult(context, correctWord, true)
                                words = words.filterNot { it.id == correctWord.id }
                            },
                            onIncorrect = { incorrectWord ->
                                // 틀린 경우 저장하고 화면에서 제거
                                FileManager.saveQuizResult(context, incorrectWord, false)
                                words = words.filterNot { it.id == incorrectWord.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

