package com.ljk.eunoia.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.ljk.eunoia.ai.GeminiApiService
import com.ljk.eunoia.data.Category
import com.ljk.eunoia.data.WordData
import com.ljk.eunoia.utils.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * 매일 자동으로 새로운 단어를 가져오는 WorkManager Worker
 */
class DailyWordWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // API 키 확인
            val prefs = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("gemini_api_key", null)
            
            if (apiKey.isNullOrBlank()) {
                // API 키가 없으면 스킵
                return@withContext Result.success()
            }
            
            // 오늘 이미 단어를 가져왔는지 확인 (수동 실행인 경우 무시)
            val forceFetch = inputData.getBoolean("force_fetch", false)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            val lastFetchDate = prefs.getString("last_word_fetch_date", null)
            
            if (!forceFetch && lastFetchDate == today) {
                // 오늘 이미 가져왔으면 스킵 (강제 실행이 아닌 경우만)
                return@withContext Result.success()
            }
            
            // 각 카테고리별로 새로운 단어 생성
            val categories = listOf("idiom", "english", "proverb", "word")
            val categoryNames = mapOf(
                "idiom" to "사자성어",
                "english" to "영어",
                "proverb" to "속담",
                "word" to "단어"
            )
            
            categories.forEach { categoryKey ->
                try {
                    android.util.Log.d("DailyWordWorker", "카테고리 처리 시작: $categoryKey")
                    
                    // 기존 단어 목록 가져오기
                    val existingCategory = FileManager.loadCategory(applicationContext, categoryKey)
                    val existingWords = existingCategory?.words?.map { it.word } ?: emptyList()
                    
                    android.util.Log.d("DailyWordWorker", "기존 단어 수: ${existingWords.size}")
                    
                    // AI로 새로운 단어 생성 (최대 5개)
                    val newWords = GeminiApiService.generateNewWords(
                        context = applicationContext,
                        existingWords = existingWords,
                        category = categoryNames[categoryKey] ?: categoryKey
                    )
                    
                    android.util.Log.d("DailyWordWorker", "생성된 새 단어 수: ${newWords.size}")
                    
                    if (newWords.isNotEmpty()) {
                        // 기존 JSON 파일에 새 단어 추가
                        addWordsToCategoryFile(categoryKey, newWords)
                        android.util.Log.d("DailyWordWorker", "단어 저장 완료: $categoryKey")
                    } else {
                        android.util.Log.w("DailyWordWorker", "생성된 단어가 없습니다: $categoryKey")
                    }
                    
                    // API 호출 간격 (무료 티어 제한 고려)
                    kotlinx.coroutines.delay(2000) // 2초 대기
                } catch (e: Exception) {
                    android.util.Log.e("DailyWordWorker", "카테고리 처리 중 오류: $categoryKey", e)
                    e.printStackTrace()
                    // 개별 카테고리 실패는 무시하고 계속 진행
                }
            }
            
            // 오늘 날짜 저장
            prefs.edit().putString("last_word_fetch_date", today).apply()
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // 실패 시 재시도
        }
    }
    
    /**
     * 카테고리 JSON 파일에 새 단어 추가
     */
    private suspend fun addWordsToCategoryFile(categoryKey: String, newWords: List<WordData>) {
        try {
            // Assets에서 기존 데이터 로드
            val existingCategory = FileManager.loadCategory(applicationContext, categoryKey)
            val existingWords = existingCategory?.words ?: emptyList()
            
            // 중복 제거 (같은 단어가 이미 있으면 제외)
            val wordsToAdd = newWords.filter { newWord ->
                existingWords.none { it.word == newWord.word }
            }
            
            if (wordsToAdd.isEmpty()) {
                return
            }
            
            // 새 단어 추가 (source="ai" 유지)
            val updatedWords = existingWords + wordsToAdd.map { 
                it.copy(
                    category = existingCategory?.category ?: categoryKey,
                    source = "ai" // AI로 생성된 단어임을 명시
                )
            }
            
            // 업데이트된 카테고리 객체 생성
            val updatedCategory = Category(
                category = existingCategory?.category ?: categoryKey,
                words = updatedWords
            )
            
            // 내부 저장소에 JSON 파일로 저장
            val gson = Gson()
            val json = gson.toJson(updatedCategory)
            
            val file = File(applicationContext.filesDir, "$categoryKey.json")
            FileWriter(file).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

