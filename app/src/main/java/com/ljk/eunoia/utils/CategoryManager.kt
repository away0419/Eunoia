package com.ljk.eunoia.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljk.eunoia.data.Category
import java.io.File
import java.util.Locale

/**
 * 단어 주제(카테고리)를 동적으로 관리하는 매니저
 */
object CategoryManager {
    private const val PREFS_NAME = "category_manager"
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
    private val gson = Gson()

    /**
     * 기본 제공 카테고리
     */
    private val defaultCategories = listOf(
        CategoryDefinition(key = "idiom", displayName = "사자성어", isDefault = true),
        CategoryDefinition(key = "proverb", displayName = "속담", isDefault = true),
        CategoryDefinition(key = "word", displayName = "단어", isDefault = true),
        CategoryDefinition(key = "english", displayName = "영어", isDefault = true)
    )

    /**
     * 카테고리 정의
     */
    data class CategoryDefinition(
        val key: String,
        val displayName: String,
        val isDefault: Boolean = false
    )

    /**
     * 모든 카테고리(기본 + 사용자 정의)를 반환
     */
    fun getAllCategories(context: Context): List<CategoryDefinition> {
        val custom = loadCustomCategories(context)
        return defaultCategories + custom
    }

    /**
     * 사용자 정의 카테고리 로드
     */
    private fun loadCustomCategories(context: Context): List<CategoryDefinition> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CUSTOM_CATEGORIES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CategoryDefinition>>() {}.type
            gson.fromJson<List<CategoryDefinition>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * 사용자 정의 카테고리 저장
     */
    private fun saveCustomCategories(context: Context, categories: List<CategoryDefinition>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, json).apply()
    }

    /**
     * 새 카테고리를 추가
     * @return 추가된 카테고리 정의 (실패 시 null)
     */
    fun addCategory(context: Context, displayName: String): CategoryDefinition? {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) {
            return null
        }

        val existing = getAllCategories(context)
        if (existing.any { it.displayName == trimmedName }) {
            // 이미 동일한 이름이 존재하면 추가하지 않음
            return null
        }

        val newKey = generateCategoryKey(trimmedName, existing.map { it.key }.toSet())
        val newDefinition = CategoryDefinition(
            key = newKey,
            displayName = trimmedName,
            isDefault = false
        )

        val custom = loadCustomCategories(context).toMutableList()
        custom.add(newDefinition)
        saveCustomCategories(context, custom)

        // 해당 카테고리용 빈 JSON 파일 생성 (내부 저장소)
        val file = File(context.filesDir, "$newKey.json")
        if (!file.exists()) {
            val emptyCategory = Category(category = trimmedName, words = emptyList())
            file.writeText(gson.toJson(emptyCategory))
        }

        return newDefinition
    }

    /**
     * 카테고리를 삭제 (기본 카테고리는 삭제 불가)
     * @return 삭제 성공 여부
     */
    fun deleteCategory(context: Context, key: String): Boolean {
        val definition = getAllCategories(context).firstOrNull { it.key == key } ?: return false
        if (definition.isDefault) {
            return false
        }

        val updatedCustom = loadCustomCategories(context)
            .filterNot { it.key == key }
        saveCustomCategories(context, updatedCustom)

        // 내부 저장소 파일 제거
        val file = File(context.filesDir, "$key.json")
        if (file.exists()) {
            file.delete()
        }

        // 히스토리에서도 해당 카테고리 단어 제거
        FileManager.removeHistoryByCategory(context, definition.displayName)

        return true
    }

    /**
     * 표시 이름으로 카테고리 키 조회
     */
    fun resolveCategoryKey(context: Context, displayNameOrKey: String): String? {
        val trimmed = displayNameOrKey.trim()
        val categories = getAllCategories(context)
        return categories.firstOrNull { it.key == trimmed }?.key
            ?: categories.firstOrNull { it.displayName == trimmed }?.key
    }

    /**
     * 키로 표시 이름 조회
     */
    fun resolveDisplayName(context: Context, keyOrDisplay: String): String {
        val trimmed = keyOrDisplay.trim()
        val categories = getAllCategories(context)
        return categories.firstOrNull { it.key == trimmed }?.displayName
            ?: categories.firstOrNull { it.displayName == trimmed }?.displayName
            ?: trimmed
    }

    /**
     * 카테고리 키 생성 (중복 방지)
     */
    private fun generateCategoryKey(displayName: String, existingKeys: Set<String>): String {
        val sanitized = displayName.trim()
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9가-힣]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
            .ifEmpty { "category" }

        var candidate = sanitized
        var index = 1
        while (existingKeys.contains(candidate)) {
            candidate = "${sanitized}_$index"
            index++
        }
        return candidate
    }
}


