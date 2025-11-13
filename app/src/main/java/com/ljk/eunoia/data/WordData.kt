package com.ljk.eunoia.data

import com.google.gson.annotations.SerializedName

/**
 * 단어 데이터 모델
 * @param word 단어
 * @param meaning 뜻
 * @param category 카테고리 (사자성어, 영어, 속담, 단어)
 * @param date 표시된 날짜
 * @param source 단어 출처 ("asset": 기본 assets, "ai": AI 생성, "user": 사용자 직접 추가)
 */
data class WordData(
    val word: String,
    val meaning: String,
    val category: String,
    val date: String? = null,
    val source: String = "asset" // 기본값은 assets에서 로드된 단어
)

/**
 * 카테고리별 단어 목록
 * @param category 카테고리 이름
 * @param words 단어 목록
 */
data class Category(
    val category: String,
    val words: List<WordData>
)

