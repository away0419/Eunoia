package com.ljk.eunoia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljk.eunoia.ai.GeminiApiService
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.ui.theme.*
import com.ljk.eunoia.utils.FileManager
import com.ljk.eunoia.utils.WorkManagerHelper
import kotlinx.coroutines.launch

/**
 * 설정 화면 - API 키 입력 및 관리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    }
    
    var apiKey by remember {
        mutableStateOf(prefs.getString("gemini_api_key", "") ?: "")
    }
    var showSuccess by remember { mutableStateOf(false) }
    
    // 단어 추가 관련 상태
    var showAddWordDialog by remember { mutableStateOf(false) }
    var wordText by remember { mutableStateOf("") }
    var meaningText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("사자성어") }
    var showWordSuccess by remember { mutableStateOf(false) }
    var showWordError by remember { mutableStateOf(false) }
    
    // 카테고리 키 매핑
    val categoryKeys = remember {
        mapOf(
            "사자성어" to "idiom",
            "영어" to "english",
            "속담" to "proverb",
            "단어" to "word"
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← 뒤로", color = PrimaryBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }
        
        // 설정 내용
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API 키 입력 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Google Gemini API 키",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    
                    Text(
                        text = "무료 티어: 월 60회 요청 가능\nAPI 키는 Google AI Studio에서 발급받을 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API 키 입력") },
                        placeholder = { Text("AIza...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (apiKey.isNotBlank()) {
                                    GeminiApiService.setApiKey(context, apiKey)
                                    showSuccess = true
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Divider
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (apiKey.isNotBlank()) {
                                GeminiApiService.setApiKey(context, apiKey)
                                showSuccess = true
                                // WorkManager 재스케줄링
                                WorkManagerHelper.scheduleDailyWordFetch(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "저장",
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    if (showSuccess) {
                        Text(
                            text = "✓ API 키가 저장되었습니다.",
                            color = PrimaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // 즉시 단어 가져오기 버튼
                    if (apiKey.isNotBlank()) {
                        var isLoadingWords by remember { mutableStateOf(false) }
                        var wordFetchResult by remember { mutableStateOf<String?>(null) }
                        
                        Button(
                            onClick = {
                                isLoadingWords = true
                                wordFetchResult = null
                                scope.launch {
                                    try {
                                        // 즉시 단어 가져오기 실행
                                        WorkManagerHelper.fetchWordsNow(context)
                                        wordFetchResult = "단어 가져오기 작업이 시작되었습니다. 잠시 후 새로고침해주세요."
                                        isLoadingWords = false
                                    } catch (e: Exception) {
                                        wordFetchResult = "오류: ${e.message}"
                                        isLoadingWords = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingWords,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoadingWords) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "🤖 지금 단어 가져오기",
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        if (wordFetchResult != null) {
                            Text(
                                text = wordFetchResult ?: "",
                                color = if (wordFetchResult?.contains("오류") == true) {
                                    androidx.compose.ui.graphics.Color(0xFFF44336)
                                } else {
                                    PrimaryBlue
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 안내 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📝 API 키 발급 방법",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "1. https://aistudio.google.com 접속\n2. Get API Key 클릭\n3. API 키 생성 및 복사\n4. 위 입력란에 붙여넣기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
            
            // 단어 추가 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "단어 직접 추가",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    
                    Text(
                        text = "원하는 단어와 뜻을 직접 추가할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    Button(
                        onClick = { showAddWordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "➕ 단어 추가하기",
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    if (showWordSuccess) {
                        Text(
                            text = "✓ 단어가 추가되었습니다.",
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (showWordError) {
                        Text(
                            text = "✗ 단어 추가에 실패했습니다. (중복된 단어일 수 있습니다.)",
                            color = androidx.compose.ui.graphics.Color(0xFFF44336),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    // 단어 추가 다이얼로그
    if (showAddWordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddWordDialog = false
                wordText = ""
                meaningText = ""
                selectedCategory = "사자성어"
                showWordSuccess = false
                showWordError = false
            },
            title = {
                Text(
                    text = "단어 추가",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 카테고리 선택
                    var expanded by remember { mutableStateOf(false) }
                    val categories = listOf("사자성어", "영어", "속담", "단어")
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("카테고리") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Divider
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 단어 입력
                    OutlinedTextField(
                        value = wordText,
                        onValueChange = { wordText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("단어") },
                        placeholder = { Text("예: 일석이조") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Divider
                        )
                    )
                    
                    // 뜻 입력
                    OutlinedTextField(
                        value = meaningText,
                        onValueChange = { meaningText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("뜻") },
                        placeholder = { Text("예: 한 가지 일로 두 가지 이익을 얻음") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Divider
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wordText.isNotBlank() && meaningText.isNotBlank()) {
                            scope.launch {
                                val categoryKey = categoryKeys[selectedCategory] as? String ?: "word"
                                val newWord = WordData(
                                    word = wordText.trim(),
                                    meaning = meaningText.trim(),
                                    category = selectedCategory,
                                    source = "user"
                                )
                                
                                val success = FileManager.addUserWord(context, newWord, categoryKey)
                                if (success) {
                                    showWordSuccess = true
                                    showWordError = false
                                    wordText = ""
                                    meaningText = ""
                                    selectedCategory = "사자성어"
                                    showAddWordDialog = false
                                } else {
                                    showWordError = true
                                    showWordSuccess = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    enabled = wordText.isNotBlank() && meaningText.isNotBlank()
                ) {
                    Text("추가", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddWordDialog = false
                    wordText = ""
                    meaningText = ""
                    selectedCategory = "사자성어"
                    showWordSuccess = false
                    showWordError = false
                }) {
                    Text("취소")
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}


