package com.ljk.eunoia.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.ljk.eunoia.data.WordData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Google Gemini API를 사용하여 새로운 단어를 생성하는 서비스
 * 무료 티어: 월 60회 요청 가능
 */
object GeminiApiService {
    // Gemini API 엔드포인트 (여러 모델과 API 버전 시도)
    // 우선순위: gemini-1.5-flash (빠름) -> gemini-1.5-pro (고품질) -> gemini-pro (기본)
    private fun getApiUrl(modelName: String, apiVersion: String = "v1beta"): String {
        return "https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent"
    }
    
    // 사용 가능한 모델 목록 (우선순위 순) - 더 많은 모델 시도
    private val availableModels = listOf(
        "gemini-1.5-flash-latest",
        "gemini-1.5-pro-latest",
        "gemini-1.5-flash",
        "gemini-1.5-pro", 
        "gemini-pro",
        "gemini-2.0-flash-exp",
        "models/gemini-1.5-flash",
        "models/gemini-1.5-pro"
    )
    
    // API 버전 목록 (우선순위 순)
    private val apiVersions = listOf("v1beta", "v1")
    
    // 성공한 모델 조합 캐시 (다음 요청 시 우선 사용)
    private var cachedWorkingModel: Pair<String, String>? = null // (apiVersion, modelName)
    
    /**
     * ListModels API를 호출하여 사용 가능한 모델 목록 가져오기
     * generateContent를 지원하는 모델만 필터링
     */
    private suspend fun getAvailableModels(context: android.content.Context): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context) ?: return@withContext emptyList()
        val client = OkHttpClient()
        
        // 여러 API 버전에서 시도
        for (apiVersion in apiVersions) {
            try {
                val url = "https://generativelanguage.googleapis.com/$apiVersion/models?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: continue
                    val gson = Gson()
                    val modelsResponse = gson.fromJson(responseBody, ModelsListResponse::class.java)
                    
                    // generateContent를 지원하는 모델만 필터링
                    val supportedModels = modelsResponse.models?.filter { model ->
                        // embedding, imagen, aqa 모델 제외
                        val name = model.name ?: ""
                        !name.contains("embedding", ignoreCase = true) &&
                        !name.contains("imagen", ignoreCase = true) &&
                        !name.contains("aqa", ignoreCase = true) &&
                        !name.contains("tts", ignoreCase = true) &&
                        !name.contains("image-generation", ignoreCase = true) &&
                        !name.contains("computer-use", ignoreCase = true) &&
                        !name.contains("robotics", ignoreCase = true) &&
                        // generateContent 메서드 지원 확인
                        (model.supportedGenerationMethods?.contains("generateContent") == true ||
                         model.supportedGenerationMethods?.contains("generate-content") == true)
                    }?.mapNotNull { it.name } ?: emptyList()
                    
                    android.util.Log.d("GeminiApiService", "generateContent 지원 모델: $supportedModels")
                    return@withContext supportedModels.map { 
                        // "models/gemini-1.5-flash" 형식에서 "gemini-1.5-flash" 추출
                        it.removePrefix("models/")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("GeminiApiService", "ListModels API 호출 실패 ($apiVersion): ${e.message}")
            }
        }
        emptyList()
    }
    
    // ListModels API 응답 모델
    private data class ModelsListResponse(
        val models: List<ModelInfo>?
    )
    
    private data class ModelInfo(
        val name: String?,
        val supportedGenerationMethods: List<String>?
    )
    
    // API 키는 SharedPreferences에 저장 (사용자가 입력)
    private fun getApiKey(context: android.content.Context): String? {
        val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("gemini_api_key", null)
    }
    
    /**
     * API 키 설정
     */
    fun setApiKey(context: android.content.Context, apiKey: String) {
        val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", apiKey).apply()
    }
    
    /**
     * AI를 사용하여 새로운 한국어 단어 생성
     * @param context 컨텍스트
     * @param existingWords 이미 저장된 단어 목록 (중복 방지용)
     * @param category 카테고리 (사자성어, 영어, 속담, 단어)
     * @return 생성된 단어 목록 (최대 5개)
     */
    suspend fun generateNewWords(
        context: android.content.Context,
        existingWords: List<String>,
        category: String
    ): List<WordData> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isNullOrBlank()) {
            android.util.Log.e("GeminiApiService", "API 키가 없습니다.")
            return@withContext emptyList()
        }
        
        android.util.Log.d("GeminiApiService", "API 요청 시작: 카테고리=$category, 기존 단어 수=${existingWords.size}")
        
        val gson = Gson() // Gson 인스턴스는 한 번만 생성
        
        try {
            // 기존 단어 목록을 명확하게 제공 (프롬프트에 포함)
            val existingWordsList = if (existingWords.isNotEmpty()) {
                existingWords.take(100).joinToString(", ")
            } else {
                "없음"
            }
            
            // 카테고리별 프롬프트 생성
            val prompt = when (category) {
                "사자성어" -> """
                    당신은 한국어 단어 추천 전문가입니다.
                    
                    **중요: 다음 조건을 반드시 지켜주세요:**
                    1. 아래에 나열된 기존 단어들은 절대 포함하지 마세요.
                    2. 최근 한국에서 많이 사용되는 사자성어를 우선적으로 추천해주세요.
                    3. 공식 사전에 존재하는 사자성어만 추천해주세요.
                    
                    **기존에 이미 사용된 사자성어 목록 (이 단어들은 제외해야 합니다):**
                    $existingWordsList
                    
                    위 목록에 없는, 최근 한국에서 많이 사용되는 사자성어 5개를 추천해주세요.
                    각 사자성어에 대해 간단하고 명확한 뜻을 함께 제공해주세요.
                    
                    응답 형식은 반드시 JSON 배열로 해주세요:
                    [
                      {"word": "사자성어1", "meaning": "뜻1"},
                      {"word": "사자성어2", "meaning": "뜻2"},
                      {"word": "사자성어3", "meaning": "뜻3"},
                      {"word": "사자성어4", "meaning": "뜻4"},
                      {"word": "사자성어5", "meaning": "뜻5"}
                    ]
                """.trimIndent()
                
                "영어" -> """
                    당신은 한국어 단어 추천 전문가입니다.
                    
                    **중요: 다음 조건을 반드시 지켜주세요:**
                    1. 아래에 나열된 기존 단어들은 절대 포함하지 마세요.
                    2. 최근 한국에서 많이 사용되는 영어 단어를 우선적으로 추천해주세요.
                    3. 공식 사전에 존재하는 영어만 추천해주세요.
                    
                    **기존에 이미 사용된 영어 단어 목록 (이 단어들은 제외해야 합니다):**
                    $existingWordsList
                    
                    위 목록에 없는, 최근 한국에서 많이 사용되는 영어 단어 5개를 추천해주세요.
                    각 단어에 대해 한국어 뜻을 함께 제공해주세요.
                    
                    응답 형식은 반드시 JSON 배열로 해주세요:
                    [
                      {"word": "영어단어1", "meaning": "한국어 뜻1"},
                      {"word": "영어단어2", "meaning": "한국어 뜻2"},
                      {"word": "영어단어3", "meaning": "한국어 뜻3"},
                      {"word": "영어단어4", "meaning": "한국어 뜻4"},
                      {"word": "영어단어5", "meaning": "한국어 뜻5"}
                    ]
                """.trimIndent()
                
                "속담" -> """
                    당신은 한국어 단어 추천 전문가입니다.
                    
                    **중요: 다음 조건을 반드시 지켜주세요:**
                    1. 아래에 나열된 기존 속담들은 절대 포함하지 마세요.
                    2. 최근 한국에서 많이 사용되는 속담을 우선적으로 추천해주세요.
                    3. 공식 사전에 존재하는 속담만 추천해주세요.
                    
                    **기존에 이미 사용된 속담 목록 (이 속담들은 제외해야 합니다):**
                    $existingWordsList
                    
                    위 목록에 없는, 최근 한국에서 많이 사용되는 속담 5개를 추천해주세요.
                    각 속담에 대해 간단하고 명확한 뜻을 함께 제공해주세요.
                    
                    응답 형식은 반드시 JSON 배열로 해주세요:
                    [
                      {"word": "속담1", "meaning": "뜻1"},
                      {"word": "속담2", "meaning": "뜻2"},
                      {"word": "속담3", "meaning": "뜻3"},
                      {"word": "속담4", "meaning": "뜻4"},
                      {"word": "속담5", "meaning": "뜻5"}
                    ]
                """.trimIndent()
                
                else -> """
                    당신은 한국어 단어 추천 전문가입니다.
                    
                    **중요: 다음 조건을 반드시 지켜주세요:**
                    1. 아래에 나열된 기존 단어들은 절대 포함하지 마세요.
                    2. **"$category" 주제에 특화된 단어만 추천해주세요.** 이 주제와 직접적으로 관련된 용어, 개념, 표현을 우선적으로 선택해주세요.
                    3. "$category" 분야에서 실제로 사용되는 전문 용어나 관련 표현을 추천해주세요.
                    4. 최근 "$category" 분야에서 트렌드가 되거나 중요하게 다뤄지는 단어를 우선적으로 추천해주세요.
                    5. 공식적으로 인정받는 용어나 개념만 추천해주세요.
                    
                    **기존에 이미 사용된 "$category" 주제 단어 목록 (이 단어들은 제외해야 합니다):**
                    $existingWordsList
                    
                    위 목록에 없는, "$category" 주제에 특화된 단어 5개를 추천해주세요.
                    각 단어에 대해 간단하고 명확한 뜻을 함께 제공해주세요.
                    
                    응답 형식은 반드시 JSON 배열로 해주세요:
                    [
                      {"word": "단어1", "meaning": "뜻1"},
                      {"word": "단어2", "meaning": "뜻2"},
                      {"word": "단어3", "meaning": "뜻3"},
                      {"word": "단어4", "meaning": "뜻4"},
                      {"word": "단어5", "meaning": "뜻5"}
                    ]
                """.trimIndent()
            }
            
            // Gemini API 요청 생성 (Gson을 사용하여 JSON을 안전하게 직렬화)
            val requestData = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )
            
            val requestBodyJson = gson.toJson(requestData)
            
            android.util.Log.d("GeminiApiService", "요청 본문: $requestBodyJson")
            
            val mediaType = "application/json".toMediaType()
            val body = requestBodyJson.toRequestBody(mediaType)
            
            // 타임아웃을 늘린 OkHttpClient (30초)
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            var lastError: String? = null
            
            // 먼저 ListModels API로 사용 가능한 모델 확인 시도
            val dynamicModels = try {
                getAvailableModels(context)
            } catch (e: Exception) {
                android.util.Log.w("GeminiApiService", "동적 모델 목록 가져오기 실패, 기본 모델 사용: ${e.message}")
                emptyList()
            }
            
            // 동적으로 가져온 모델이 있으면 우선 사용, 없으면 기본 모델 목록 사용
            val modelsToTry = if (dynamicModels.isNotEmpty()) {
                android.util.Log.d("GeminiApiService", "동적으로 가져온 모델 사용: $dynamicModels")
                dynamicModels + availableModels // 동적 모델 + 기본 모델
            } else {
                android.util.Log.d("GeminiApiService", "기본 모델 목록 사용: $availableModels")
                availableModels
            }
            
            // 캐시된 성공 모델이 있으면 우선 시도
            val modelsAndVersions = if (cachedWorkingModel != null) {
                val (cachedVersion, cachedModel) = cachedWorkingModel!!
                android.util.Log.d("GeminiApiService", "캐시된 모델 우선 시도: $cachedVersion/$cachedModel")
                listOf(Pair(cachedVersion, cachedModel)) + 
                apiVersions.flatMap { version -> modelsToTry.map { Pair(version, it) } }
            } else {
                apiVersions.flatMap { version -> modelsToTry.map { Pair(version, it) } }
            }
            
            // 여러 모델과 API 버전을 시도 (첫 번째로 작동하는 조합 사용)
            for ((apiVersion, modelName) in modelsAndVersions) {
                try {
                    val apiUrl = getApiUrl(modelName, apiVersion)
                    val request = Request.Builder()
                        .url("$apiUrl?key=$apiKey")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build()
                    
                    android.util.Log.d("GeminiApiService", "API 시도: $apiVersion/$modelName, URL=${request.url}")
                    val response = client.newCall(request).execute()
                    
                    android.util.Log.d("GeminiApiService", "HTTP 응답: code=${response.code}, success=${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        // 성공한 모델 조합 캐시
                        cachedWorkingModel = Pair(apiVersion, modelName)
                        android.util.Log.d("GeminiApiService", "성공한 조합 (캐시됨): $apiVersion/$modelName")
                        // 성공한 경우 응답 처리로 진행
                        val responseBody = response.body?.string()
                        if (responseBody == null || responseBody.isEmpty()) {
                            android.util.Log.e("GeminiApiService", "응답 본문이 비어있습니다.")
                            continue // 다음 모델 시도
                        }
                        
                        android.util.Log.d("GeminiApiService", "응답 본문 길이: ${responseBody.length}")
                        android.util.Log.d("GeminiApiService", "응답 본문 (처음 500자): ${responseBody.take(500)}")
                        
                        // JSON 응답 파싱
                        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                        
                        // 텍스트에서 JSON 배열 추출
                        val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (text == null || text.isEmpty()) {
                            android.util.Log.e("GeminiApiService", "응답에서 텍스트를 찾을 수 없습니다. 응답: $responseBody")
                            continue // 다음 모델 시도
                        }
                        
                        android.util.Log.d("GeminiApiService", "추출된 텍스트 길이: ${text.length}")
                        
                        // JSON 배열 부분만 추출 (```json ... ``` 또는 [...] 형식)
                        var jsonStart = text.indexOf('[')
                        var jsonEnd = text.lastIndexOf(']')
                        
                        // ```json ... ``` 형식 처리
                        if (jsonStart == -1) {
                            val codeBlockStart = text.indexOf("```json")
                            val codeBlockEnd = text.indexOf("```", codeBlockStart + 7)
                            if (codeBlockStart != -1 && codeBlockEnd != -1) {
                                val codeBlock = text.substring(codeBlockStart + 7, codeBlockEnd).trim()
                                jsonStart = codeBlock.indexOf('[')
                                jsonEnd = codeBlock.lastIndexOf(']')
                                if (jsonStart != -1 && jsonEnd != -1) {
                                    val jsonArray = codeBlock.substring(jsonStart, jsonEnd + 1)
                                    try {
                                        val words = gson.fromJson(jsonArray, Array<WordDataJson>::class.java)
                                        val result = words.map { wordJson ->
                                            WordData(
                                                word = wordJson.word,
                                                meaning = wordJson.meaning,
                                                category = category,
                                                date = null,
                                                source = "ai"
                                            )
                                        }
                                        android.util.Log.d("GeminiApiService", "생성된 단어 수: ${result.size}")
                                        return@withContext result
                                    } catch (e: Exception) {
                                        android.util.Log.e("GeminiApiService", "JSON 파싱 오류", e)
                                        continue
                                    }
                                }
                            }
                        }
                        
                        if (jsonStart == -1 || jsonEnd == -1) {
                            android.util.Log.e("GeminiApiService", "JSON 배열을 찾을 수 없습니다.")
                            continue
                        }
                        
                        val jsonArray = text.substring(jsonStart, jsonEnd + 1)
                        val words = gson.fromJson(jsonArray, Array<WordDataJson>::class.java)
                        
                        // WordData로 변환
                        val result = words.map { wordJson ->
                            WordData(
                                word = wordJson.word,
                                meaning = wordJson.meaning,
                                category = category,
                                date = null,
                                source = "ai"
                            )
                        }
                        
                        android.util.Log.d("GeminiApiService", "생성된 단어 수: ${result.size}")
                        return@withContext result
                    } else {
                        // 실패한 경우 에러 저장하고 다음 모델 시도
                        val errorBody = response.body?.string() ?: "Unknown error"
                        lastError = "$apiVersion/$modelName: code=${response.code}, body=$errorBody"
                        
                        // 429 에러인 경우 캐시된 모델 무효화 (할당량 초과)
                        if (response.code == 429) {
                            if (cachedWorkingModel?.first == apiVersion && cachedWorkingModel?.second == modelName) {
                                cachedWorkingModel = null
                                android.util.Log.w("GeminiApiService", "할당량 초과로 캐시 무효화: $apiVersion/$modelName")
                            }
                        }
                        
                        android.util.Log.w("GeminiApiService", "API 조합 실패: $lastError")
                        response.body?.close() // 리소스 정리
                    }
                } catch (e: Exception) {
                    lastError = "$apiVersion/$modelName: ${e.message}"
                    android.util.Log.w("GeminiApiService", "API 조합 예외 발생: $lastError", e)
                    continue // 다음 모델 시도
                }
            }
            
            // 모든 모델과 API 버전 실패
            android.util.Log.e("GeminiApiService", "모든 API 조합 시도 실패. 마지막 에러: $lastError")
            return@withContext emptyList()
        } catch (e: Exception) {
            android.util.Log.e("GeminiApiService", "API 요청 중 오류 발생", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Gemini API 응답 모델
    private data class GeminiResponse(
        val candidates: List<Candidate>?
    )
    
    private data class Candidate(
        val content: Content?
    )
    
    private data class Content(
        val parts: List<Part>?
    )
    
    private data class Part(
        val text: String?
    )
    
    // JSON 파싱용 데이터 클래스
    private data class WordDataJson(
        val word: String,
        val meaning: String
    )
}

