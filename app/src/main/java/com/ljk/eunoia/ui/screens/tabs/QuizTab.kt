package com.ljk.eunoia.ui.screens.tabs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.components.QuizWordCard
import com.ljk.eunoia.ui.theme.*
import com.ljk.eunoia.utils.FileManager

/**
 * 퀴즈 탭 - 단어나 뜻을 블러 처리하고 탭하여 확인 (토스 스타일)
 */
@Composable
fun QuizTab() {
    val context = LocalContext.current
    var words by remember { mutableStateOf<List<WordData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            words = FileManager.getQuizWords(context)
        } catch (e: Exception) {
            e.printStackTrace()
            words = emptyList()
        } finally {
            isLoading = false
        }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(words) { word ->
                        QuizWordCard(word = word)
                    }
                }
            }
        }
    }
}

