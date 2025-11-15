package com.ljk.eunoia.ui.screens.tabs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.components.WordCard
import com.ljk.eunoia.ui.theme.*
import com.ljk.eunoia.utils.CategoryManager
import com.ljk.eunoia.utils.FileManager
import kotlinx.coroutines.launch

/**
 * 지난 단어 탭 (토스 스타일)
 * 주제별로 그룹화하여 표시
 */
@Composable
fun HistoryTab() {
    val context = LocalContext.current
    var words by remember { mutableStateOf<List<WordData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // 선택된 주제 (null이면 주제 목록, 값이 있으면 해당 주제의 단어)
    val scope = rememberCoroutineScope()
    
    // 주제별 단어 개수 계산
    val categoriesWithCount = remember(words) {
        val allCategories = CategoryManager.getAllCategories(context)
        allCategories.map { categoryDef ->
            val count = words.count { it.category == categoryDef.displayName }
            Pair(categoryDef.displayName, count)
        }.filter { it.second > 0 } // 단어가 있는 주제만 표시
    }
    
    // 선택된 주제의 단어 목록
    val filteredWords = remember(selectedCategory, words) {
        if (selectedCategory == null) {
            emptyList()
        } else {
            words.filter { it.category == selectedCategory }
        }
    }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // 히스토리 단어 가져오기
                val historyWords = FileManager.getHistoryWords(context)
                
                // 기본 asset 단어들도 가져오기
                val allCategories = CategoryManager.getAllCategories(context)
                val assetWordsList = mutableListOf<WordData>()
                
                allCategories.forEach { categoryDef ->
                    try {
                        val category = FileManager.loadCategory(context, categoryDef.key)
                        category?.words?.forEach { word ->
                            val source = word.source ?: ""
                            // asset 단어만 추가 (AI나 사용자 추가 단어는 히스토리에 있으므로 제외)
                            if (source == "asset" || source.isEmpty()) {
                                assetWordsList.add(
                                    word.copy(
                                        category = categoryDef.displayName,
                                        source = "asset"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // 히스토리 단어와 asset 단어 합치기
                // 중복 제거: 같은 word+meaning이 있으면 히스토리 것을 우선 (히스토리가 더 최신 정보)
                val combinedWords = mutableListOf<WordData>()
                val historyWordKeys = historyWords.map { "${it.word}|${it.meaning}" }.toSet()
                
                // 히스토리 단어 추가
                combinedWords.addAll(historyWords)
                
                // asset 단어 추가 (히스토리에 없는 것만)
                assetWordsList.forEach { assetWord ->
                    val key = "${assetWord.word}|${assetWord.meaning}"
                    if (!historyWordKeys.contains(key)) {
                        combinedWords.add(assetWord)
                    }
                }
                
                words = combinedWords
            } catch (e: Exception) {
                e.printStackTrace()
                words = emptyList()
            } finally {
                isLoading = false
            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        top = 24.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        end = 20.dp,
                        bottom = 24.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 뒤로가기 버튼 (주제 선택 시에만 표시)
                if (selectedCategory != null) {
                    IconButton(
                        onClick = { selectedCategory = null }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column {
                    Text(
                        text = if (selectedCategory != null) selectedCategory!! else "지난 단어",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedCategory != null) {
                            "${filteredWords.size}개의 단어"
                        } else {
                            "지금까지 학습한 단어들을 확인하세요"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // 내용 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryBlue
                )
            } else if (selectedCategory == null) {
                // 주제 목록 표시
                if (categoriesWithCount.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "지난 단어가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    ) {
                        items(categoriesWithCount) { (categoryName, count) ->
                            // 주제 카드
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = categoryName },
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${count}개의 단어",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    // 화살표 아이콘
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "이동",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 선택된 주제의 단어 목록 표시
                if (filteredWords.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "이 주제에 단어가 없습니다",
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
                            items = filteredWords,
                            key = { it.id }
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
                                                words = words.filterNot { it.id == targetWord.id }
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
}

