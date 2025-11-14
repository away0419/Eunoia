package com.ljk.eunoia.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljk.eunoia.data.Category
import com.ljk.eunoia.data.WordData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * 파일에서 단어 데이터를 관리하는 유틸리티 클래스
 */
object FileManager {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Assets 폴더 또는 내부 저장소에서 카테고리별 단어 목록을 로드
     * 내부 저장소에 파일이 있으면 우선 사용, 없으면 Assets에서 로드
     * @param context 컨텍스트
     * @param categoryName 카테고리 이름 (파일명과 동일)
     * @return 카테고리 객체
     */
    suspend fun loadCategory(context: Context, categoryKey: String): Category? = withContext(Dispatchers.IO) {
        try {
            val definition = CategoryManager.getAllCategories(context)
                .firstOrNull { it.key == categoryKey || it.displayName == categoryKey }
            val actualKey = definition?.key ?: categoryKey
            val displayName = definition?.displayName ?: categoryKey

            // 먼저 내부 저장소에서 확인 (AI로 추가된 단어가 있을 수 있음)
            val internalFile = java.io.File(context.filesDir, "$actualKey.json")
            if (internalFile.exists()) {
                try {
                    val json = internalFile.readText()
                    val type = object : TypeToken<Category>() {}.type
                    val internalCategory = gson.fromJson<Category>(json, type)?.copy(category = displayName)
                    if (internalCategory != null) {
                        return@withContext internalCategory
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 기본 카테고리는 Assets에서 로드
            if (definition?.isDefault == true) {
                val json = context.assets.open("$actualKey.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<Category>() {}.type
                return@withContext gson.fromJson<Category>(json, type)?.copy(category = displayName)
            }

            // 사용자 정의 카테고리는 빈 카테고리 반환
            Category(
                category = displayName,
                words = emptyList()
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 오늘 날짜에 해당하는 단어들을 가져옴 (각 카테고리에서 5개씩)
     * 우선순위: 오늘 날짜의 AI 단어 > 오늘 날짜의 사용자 추가 단어 > 기본 asset 단어
     * @param context 컨텍스트
     * @return 오늘의 단어 목록
     */
    suspend fun getTodayWords(context: Context): List<WordData> = withContext(Dispatchers.IO) {
        try {
            val today = dateFormat.format(Date())
            val categoryDefinitions = CategoryManager.getAllCategories(context)
            val allWords = mutableListOf<WordData>()

            categoryDefinitions.forEach { categoryDefinition ->
                try {
                    val category = loadCategory(context, categoryDefinition.key)
                    category?.words?.let { words ->
                        if (words.isNotEmpty()) {
                            // 1. 오늘 날짜의 AI 단어들 (우선순위 1)
                            val todayAiWords = words.filter { 
                                val source = it.source ?: ""
                                source == "ai" && it.date == today
                            }
                            
                            // 2. 오늘 날짜의 사용자 추가 단어들 (우선순위 2)
                            val todayUserWords = words.filter { 
                                val source = it.source ?: ""
                                source == "user" && it.date == today
                            }
                            
                            // 3. 기본 asset 단어들 (우선순위 3)
                            val assetWords = words.filter { 
                                val source = it.source ?: ""
                                source == "asset" || source.isEmpty()
                            }
                            
                            // 오늘 날짜의 AI/사용자 단어 합치기
                            val todayWords = (todayAiWords + todayUserWords).take(5)
                            
                            // 오늘 날짜 단어가 5개 미만이면 asset 단어로 채우기
                            val remainingCount = 5 - todayWords.size
                            val selectedAssetWords = if (remainingCount > 0 && assetWords.isNotEmpty()) {
                                // 날짜 기반으로 asset 단어 선택 (일관성 유지)
                                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                                val startIndex = (dayOfYear * 5) % assetWords.size
                                val endIndex = minOf(startIndex + remainingCount, assetWords.size)
                                
                                if (startIndex < assetWords.size) {
                                    assetWords.subList(startIndex, endIndex)
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                            
                            // 최종 선택된 단어들 합치기
                            val selectedWords = todayWords + selectedAssetWords
                            
                            selectedWords.forEach { word ->
                                // 카테고리 정보를 포함하여 복사 (source가 null이거나 비어있으면 기본값 "asset" 사용)
                                allWords.add(
                                    word.copy(
                                        category = categoryDefinition.displayName,
                                        date = today,
                                        source = word.source.takeIf { !it.isNullOrEmpty() } ?: "asset" // source가 null이거나 비어있으면 기본값 사용
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 개별 카테고리 로드 실패는 무시하고 계속 진행
                }
            }

            allWords
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * SharedPreferences에 저장된 지난 단어들을 가져옴
     * @param context 컨텍스트
     * @return 지난 단어 목록
     */
    fun getHistoryWords(context: Context): List<WordData> {
        val prefs = context.getSharedPreferences("word_history", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("history", "[]") ?: "[]"
        val type = object : TypeToken<List<WordData>>() {}.type
        return try {
            gson.fromJson<List<WordData>>(historyJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 퀴즈용 단어들을 가져옴
     * 날짜별 확률에 따라 선택
     * 오늘: 30%, 하루 전: 30%, 이틀 전: 20%, 사흘 전: 10%, 나머지: 10%
     * 출현 횟수를 비슷하게 유지하는 알고리즘 적용
     * @param context 컨텍스트
     * @return 퀴즈용 단어 목록 (최대 30개)
     */
    suspend fun getQuizWords(context: Context): List<WordData> = withContext(Dispatchers.IO) {
        try {
            val today = dateFormat.format(Date())
            
            // 날짜별 단어 그룹화
            val wordsByDate = mutableMapOf<String, MutableList<WordData>>()
            
            // 히스토리 단어 가져오기
            val historyWords = getHistoryWords(context)
            
            // 오늘 날짜 포함하여 날짜별로 그룹화
            historyWords.forEach { word ->
                val wordDate = word.date ?: return@forEach
                wordsByDate.getOrPut(wordDate) { mutableListOf() }.add(word)
            }
            
            if (wordsByDate.isEmpty()) {
                return@withContext emptyList()
            }
            
            // 날짜를 날짜 순으로 정렬 (최신순)
            val sortedDates = wordsByDate.keys.sortedDescending()
            
            // 날짜별 가중치 계산
            val dateWeights = mutableMapOf<String, Double>()
            sortedDates.forEachIndexed { index, date ->
                val weight = when (index) {
                    0 -> 0.30 // 오늘
                    1 -> 0.30 // 하루 전
                    2 -> 0.20 // 이틀 전
                    3 -> 0.10 // 사흘 전
                    else -> 0.10 // 나머지
                }
                dateWeights[date] = weight
            }
            
            // 전체 가중치 합계 계산
            val totalWeight = dateWeights.values.sum()
            
            // 각 단어의 선택 횟수 추적 (SharedPreferences 사용)
            val prefs = context.getSharedPreferences("quiz_word_counts", Context.MODE_PRIVATE)
            val wordCountsJson = prefs.getString("counts", "{}") ?: "{}"
            val wordCountsType = object : TypeToken<Map<String, Int>>() {}.type
            val wordCounts = try {
                gson.fromJson<Map<String, Int>>(wordCountsJson, wordCountsType) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }.toMutableMap()
            
            // 단어별 고유 키 생성 (word + meaning + category)
            fun getWordKey(word: WordData): String {
                return "${word.word}|${word.meaning}|${word.category}"
            }
            
            // 선택된 단어 목록
            val selectedWords = mutableListOf<WordData>()
            val maxWords = 30
            
            // 가중치 기반 선택 알고리즘 (출현 횟수 고려)
            repeat(maxWords) {
                // 날짜별로 가중치에 따라 선택
                val random = Random.nextDouble() * totalWeight
                var cumulativeWeight = 0.0
                var selectedDate: String? = null
                
                for ((date, weight) in dateWeights) {
                    cumulativeWeight += weight
                    if (random <= cumulativeWeight) {
                        selectedDate = date
                        break
                    }
                }
                
                // 선택된 날짜가 없으면 첫 번째 날짜 사용
                selectedDate = selectedDate ?: sortedDates.firstOrNull()
                if (selectedDate == null) {
                    return@repeat // break 대신 return@repeat 사용
                }
                
                val dateWords = wordsByDate[selectedDate]
                if (dateWords == null || dateWords.isEmpty()) {
                    return@repeat // continue 대신 return@repeat 사용
                }
                
                // 출현 횟수를 고려하여 선택 (적게 나온 단어 우선)
                val wordWithCounts = dateWords.map { word ->
                    val key = getWordKey(word)
                    val count = wordCounts[key] ?: 0
                    Pair(word, count)
                }
                
                // 최소 출현 횟수를 가진 단어들 중에서 선택
                val minCount = wordWithCounts.minOfOrNull { it.second } ?: 0
                val candidates = wordWithCounts.filter { it.second == minCount }
                
                // 후보가 있으면 랜덤 선택, 없으면 전체에서 랜덤 선택
                val selected = if (candidates.isNotEmpty()) {
                    candidates.random().first
                } else {
                    dateWords.random()
                }
                
                selectedWords.add(selected)
                
                // 선택 횟수 업데이트
                val key = getWordKey(selected)
                wordCounts[key] = (wordCounts[key] ?: 0) + 1
            }
            
            // 선택 횟수 저장
            val updatedCountsJson = gson.toJson(wordCounts)
            prefs.edit().putString("counts", updatedCountsJson).apply()
            
            selectedWords
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 단어를 히스토리에 저장
     * @param context 컨텍스트
     * @param word 저장할 단어
     */
    fun saveToHistory(context: Context, word: WordData) {
        val prefs = context.getSharedPreferences("word_history", Context.MODE_PRIVATE)
        val history = getHistoryWords(context).toMutableList()
        
        // 중복 제거 (같은 날짜의 같은 단어)
        history.removeAll { it.word == word.word && it.date == word.date }
        history.add(0, word) // 최신순으로 추가
        
        // 최대 1000개까지만 저장
        if (history.size > 1000) {
            history.removeAt(history.size - 1)
        }
        
        val historyJson = gson.toJson(history)
        prefs.edit().putString("history", historyJson).apply()
    }
    
    /**
     * 히스토리에서 단어를 삭제
     * @param context 컨텍스트
     * @param word 삭제할 단어
     */
    fun removeFromHistory(context: Context, word: WordData) {
        val prefs = context.getSharedPreferences("word_history", Context.MODE_PRIVATE)
        val history = getHistoryWords(context).toMutableList()
        history.removeAll { historyWord ->
            historyWord.word == word.word &&
                    historyWord.category == word.category &&
                    (word.date == null || historyWord.date == word.date)
        }
        val historyJson = gson.toJson(history)
        prefs.edit().putString("history", historyJson).apply()
    }

    /**
     * 히스토리에서 특정 카테고리의 단어를 모두 삭제
     * @param context 컨텍스트
     * @param categoryDisplayName 카테고리 표시 이름
     */
    fun removeHistoryByCategory(context: Context, categoryDisplayName: String) {
        val prefs = context.getSharedPreferences("word_history", Context.MODE_PRIVATE)
        val history = getHistoryWords(context)
        if (history.isEmpty()) {
            return
        }

        val updatedHistory = history.filterNot { it.category == categoryDisplayName }
        if (updatedHistory.size == history.size) {
            return
        }

        val historyJson = gson.toJson(updatedHistory)
        prefs.edit().putString("history", historyJson).apply()
    }
    
    /**
     * 사용자가 직접 추가한 단어를 카테고리 파일에 저장
     * @param context 컨텍스트
     * @param word 저장할 단어 (source="user"로 설정되어야 함)
     * @param categoryKey 카테고리 키 (idiom, english, proverb, word)
     */
    suspend fun addUserWord(context: Context, word: WordData, categoryKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 기존 카테고리 로드
            val existingCategory = loadCategory(context, categoryKey)
            val existingWords = existingCategory?.words?.toMutableList() ?: mutableListOf()
            val displayName = CategoryManager.resolveDisplayName(context, categoryKey)
            
            // 중복 확인 (같은 단어가 이미 있으면 실패)
            if (existingWords.any { it.word == word.word }) {
                return@withContext false
            }
            
            // 오늘 날짜 가져오기
            val today = dateFormat.format(Date())
            
            // 사용자 추가 단어 생성 (source="user"로 설정, 날짜 추가)
            val userWord = word.copy(
                category = displayName,
                source = "user",
                date = today
            )
            
            // 새 단어 추가
            existingWords.add(userWord)
            
            // 업데이트된 카테고리 객체 생성
            val updatedCategory = Category(
                category = displayName,
                words = existingWords
            )
            
            // 내부 저장소에 JSON 파일로 저장
            val json = gson.toJson(updatedCategory)
            val file = java.io.File(context.filesDir, "$categoryKey.json")
            java.io.FileWriter(file).use { writer ->
                writer.write(json)
            }
            
            // 히스토리에 자동으로 저장
            saveToHistory(context, userWord)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 카테고리 파일과 히스토리에서 단어를 삭제
     * @param context 컨텍스트
     * @param word 삭제할 단어
     */
    suspend fun deleteWord(context: Context, word: WordData): Boolean = withContext(Dispatchers.IO) {
        try {
            // asset 단어는 삭제 불가
            val source = word.source ?: ""
            if (source == "asset" || source.isEmpty()) {
                return@withContext false
            }
            
            val categoryKey = CategoryManager.resolveCategoryKey(context, word.category) ?: return@withContext false
            val existingCategory = loadCategory(context, categoryKey)
            val existingWords = existingCategory?.words?.toMutableList() ?: return@withContext false

            // 같은 단어(단어 + 뜻 기준)를 제거
            val removed = existingWords.removeAll { target ->
                target.word == word.word && target.meaning == word.meaning
            }

            if (!removed) {
                return@withContext false
            }

            // 업데이트된 카테고리 데이터를 내부 저장소에 저장
            val updatedCategory = Category(
                category = existingCategory.category,
                words = existingWords
            )

            val json = gson.toJson(updatedCategory)
            val file = java.io.File(context.filesDir, "$categoryKey.json")
            java.io.FileWriter(file).use { writer ->
                writer.write(json)
            }

            // 히스토리에서도 동일한 단어 제거
            removeFromHistory(context, word)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

