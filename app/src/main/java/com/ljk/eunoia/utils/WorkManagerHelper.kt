package com.ljk.eunoia.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ljk.eunoia.workers.DailyWordWorker
import java.util.concurrent.TimeUnit

/**
 * WorkManager를 사용하여 매일 자동으로 단어를 가져오는 작업을 스케줄링
 */
object WorkManagerHelper {
    private const val WORK_NAME = "daily_word_fetch"
    
    /**
     * 매일 자동으로 단어를 가져오는 작업 스케줄링
     * 매일 오전 9시에 실행 (최소 15분 간격으로 체크)
     */
    fun scheduleDailyWordFetch(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 인터넷 연결 필요
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<DailyWordWorker>(
            1, TimeUnit.DAYS // 1일마다 실행
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS) // 다음 오전 9시까지 대기
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 이미 있으면 유지
            workRequest
        )
    }
    
    /**
     * 다음 오전 9시까지의 대기 시간 계산
     */
    private fun calculateInitialDelay(): Long {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        
        // 오전 9시 설정
        val targetHour = 9
        val targetMinute = 0
        
        // 오늘 오전 9시까지의 시간 계산
        calendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(java.util.Calendar.MINUTE, targetMinute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // 이미 오전 9시가 지났으면 내일 오전 9시로 설정
        if (currentHour > targetHour || (currentHour == targetHour && currentMinute >= targetMinute)) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        return if (delay > 0) delay else 0
    }
    
    /**
     * 작업 취소
     */
    fun cancelDailyWordFetch(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    /**
     * 즉시 단어를 가져오는 작업 실행 (수동 실행)
     * 오늘 날짜 체크를 무시하고 강제로 실행
     */
    fun fetchWordsNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 인터넷 연결 필요
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<DailyWordWorker>()
            .setConstraints(constraints)
            .setInputData(androidx.work.Data.Builder()
                .putBoolean("force_fetch", true) // 강제 실행 플래그
                .build())
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}


