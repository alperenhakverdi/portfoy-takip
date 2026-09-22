package com.portfoy.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Şemanın ilk değişikliği: `price_quote`'a günlük değişim yüzdesi (`changePercent`) eklendi.
 * Var olan satırlar `NULL` kalır (o an için kaynak verisi yoktu); bir sonraki tazelemede dolar.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE price_quote ADD COLUMN changePercent TEXT")
    }
}
