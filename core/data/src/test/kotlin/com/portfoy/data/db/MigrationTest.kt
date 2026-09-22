package com.portfoy.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * İlk şema değişikliği (v1 → v2, `changePercent` sütunu). Plan bunun ilk migration testi olmasını
 * öngörmüştü ("Migration testi ilk şema değişikliğinde"): eski veritabanı bozulmadan açılmalı,
 * var olan satırlar korunmalı, yeni sütun `NULL` gelmeli.
 *
 * `androidx.room.testing.MigrationTestHelper` bu Room + Robolectric sürüm kombinasyonunda çalışmıyor
 * (`SupportSQLiteDriver` içinde yol karşılaştırması hatalı: "configured to open ... but ... was
 * requested" — bilinen bir uyumsuzluk). Bunun yerine [MIGRATION_1_2] doğrudan ham bir veritabanına
 * uygulanır; test edilen şey zaten migration'ın kendisidir, yardımcı sınıf değil.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @Test
    fun `v1 den v2 ye gecis eski kaydi korur, yeni sutunu null birakir`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // bellekte, dosyaya yazmaz
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // v1 şemasının aynısı (schemas/1.json'daki createSql'den).
                            db.execSQL(
                                "CREATE TABLE asset (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, code TEXT NOT NULL, " +
                                    "name TEXT NOT NULL, category TEXT NOT NULL, currency TEXT NOT NULL, unitType TEXT NOT NULL, " +
                                    "exchange TEXT, active INTEGER NOT NULL, fundKind TEXT, searchText TEXT NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE price_quote (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, assetId INTEGER NOT NULL, " +
                                    "price TEXT NOT NULL, currency TEXT NOT NULL, priceTl TEXT NOT NULL, timestamp INTEGER NOT NULL, " +
                                    "source TEXT NOT NULL)",
                            )
                        }

                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )

        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO asset (id, code, name, category, currency, unitType, searchText, active, exchange, fundKind) " +
                "VALUES (1, 'THYAO', 'Türk Hava Yolları', 'BIST', 'TRY', 'ADET', 'thyao turk hava yollari', 1, NULL, NULL)",
        )
        db.execSQL(
            "INSERT INTO price_quote (id, assetId, price, currency, priceTl, timestamp, source) " +
                "VALUES (1, 1, '293.50', 'TRY', '293.50', 1700000000000, 'YAHOO')",
        )

        MIGRATION_1_2.migrate(db)

        val satir = db.query("SELECT price, changePercent FROM price_quote WHERE id = 1")
        satir.use {
            assertTrue(it.moveToFirst())
            assertEquals("293.50", it.getString(it.getColumnIndexOrThrow("price")))
            assertTrue(it.isNull(it.getColumnIndexOrThrow("changePercent")))
        }
        // Eski varlık kaydı da bozulmadan duruyor.
        val varlik = db.query("SELECT code FROM asset WHERE id = 1")
        varlik.use {
            assertTrue(it.moveToFirst())
            assertEquals("THYAO", it.getString(0))
        }
        db.close()
    }
}
