package com.portfoy.data.db

import com.portfoy.calc.normalizeForSearch
import com.portfoy.model.Category
import com.portfoy.model.UnitType

/** Her kurulumda bulunan sabit varlıklar: Nakit TL, gram altın/gümüş ve dövizler. */
object VarsayilanVarliklar {
    const val NAKIT_KODU = "TRY"
    const val ALTIN_KODU = "XAUGR"
    const val GUMUS_KODU = "XAGGR"
    const val DOLAR_KODU = "USDTRY"
    const val EURO_KODU = "EURTRY"

    private fun entity(code: String, name: String, category: Category, unit: UnitType) = AssetEntity(
        code = code,
        name = name,
        category = category,
        currency = "TRY",
        unitType = unit,
        searchText = normalizeForSearch("$code $name"),
    )

    val liste: List<AssetEntity> = listOf(
        entity(NAKIT_KODU, "Türk Lirası (nakit)", Category.NAKIT, UnitType.TL),
        entity(ALTIN_KODU, "Gram Altın", Category.EMTIA, UnitType.GRAM),
        entity(GUMUS_KODU, "Gram Gümüş", Category.EMTIA, UnitType.GRAM),
        // Döviz: miktar para biriminin kendisidir, fiyatı kurun TL karşılığıdır.
        entity(DOLAR_KODU, "Amerikan Doları", Category.DOVIZ, UnitType.BIRIM),
        entity(EURO_KODU, "Euro", Category.DOVIZ, UnitType.BIRIM),
    )

    /** İlk açılışta çağrılır; kayıtlar varsa dokunmaz, bu yüzden her açılışta çağrılması güvenlidir. */
    suspend fun ekle(dao: AssetDao) {
        dao.insertAll(liste)
    }
}
