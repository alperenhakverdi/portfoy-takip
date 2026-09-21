package com.portfoy.calc

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{M}+")

/**
 * Arama için metni Türkçe karakterlerden bağımsız hale getirir: küçük harfe çevirir ve
 * ç/ğ/ı/İ/ö/ş/ü karakterlerini ASCII karşılıklarına indirger ("Türk" ≈ "turk").
 */
fun normalizeForSearch(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace('ı', 'i') // noktasız ı ayrışmaz, elle çevrilir
        .replace('İ', 'i')
    return combiningMarks.replace(decomposed, "").lowercase(Locale.ROOT).trim()
}

/** Aramanın başlaması için gereken en az karakter sayısı. */
const val MIN_SEARCH_LENGTH = 2
