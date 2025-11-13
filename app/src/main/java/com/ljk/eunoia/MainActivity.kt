package com.ljk.eunoia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ljk.eunoia.ui.screens.MainScreen
import com.ljk.eunoia.ui.theme.Eunoia_androidTheme
import com.ljk.eunoia.utils.WorkManagerHelper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 매일 자동으로 단어를 가져오는 작업 스케줄링
        WorkManagerHelper.scheduleDailyWordFetch(this)
        
        // 오늘 처음 앱을 켰는지 확인하고, API 키가 있으면 자동으로 단어 가져오기
        checkAndFetchWordsIfNeeded()
        
        setContent {
            Eunoia_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    /**
     * 오늘 처음 앱을 켰는지 확인하고, 필요하면 자동으로 단어 가져오기
     */
    private fun checkAndFetchWordsIfNeeded() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", null)
        
        // API 키가 없으면 스킵
        if (apiKey.isNullOrBlank()) {
            return
        }
        
        // 오늘 날짜 확인
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastAppOpenDate = prefs.getString("last_app_open_date", null)
        val lastFetchDate = prefs.getString("last_word_fetch_date", null)
        
        // 오늘 처음 앱을 켰고, 오늘 아직 단어를 가져오지 않았으면 자동으로 가져오기
        if (lastAppOpenDate != today && lastFetchDate != today) {
            // 오늘 날짜 저장
            prefs.edit().putString("last_app_open_date", today).apply()
            
            // 자동으로 단어 가져오기 실행
            WorkManagerHelper.fetchWordsNow(this)
        } else if (lastAppOpenDate != today) {
            // 오늘 처음 앱을 켰지만 이미 단어를 가져왔으면 날짜만 업데이트
            prefs.edit().putString("last_app_open_date", today).apply()
        }
    }
}
