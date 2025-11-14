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
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.components.WordCard
import com.ljk.eunoia.ui.screens.TossIconButton
import com.ljk.eunoia.ui.theme.*
import com.ljk.eunoia.utils.FileManager
import kotlinx.coroutines.launch

/**
 * 오늘의 단어 탭 (토스 스타일)
 */
@Composable
fun TodayTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var words by remember { mutableStateOf<List<WordData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 단어 로드 함수
    suspend fun loadWords() {
        try {
            words = FileManager.getTodayWords(context)
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
                        text = "오늘의 단어",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "매일 새로운 단어를 만나보세요",
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
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryBlue
                    )
                }
            } else if (words.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "헤더의 새로고침 버튼을 눌러주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = words,
                        key = { "${it.category}-${it.word}-${it.source}" }
                    ) { word ->
                        // asset 단어는 삭제 불가
                        val canDelete = word.source != "asset" && (word.source?.isNotEmpty() == true)
                        
                        WordCard(
                            word = word,
                            showDate = true,
                            onDelete = if (canDelete) {
                                { targetWord ->
                                    // 단어 삭제 요청 처리
                                    scope.launch {
                                        val success = FileManager.deleteWord(context, targetWord)
                                        if (success) {
                                            words = words.filterNot { removed ->
                                                removed.word == targetWord.word && removed.category == targetWord.category
                                            }
                                        }
                                    }
                                }
                            } else {
                                null // asset 단어는 삭제 불가
                            }
                        )
                    }
                }
            }
        }
    }
}

