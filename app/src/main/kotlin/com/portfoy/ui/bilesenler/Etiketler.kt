package com.portfoy.ui.bilesenler

import com.portfoy.model.Category
import com.portfoy.model.UnitType

fun Category.etiket(): String = when (this) {
    Category.ABD -> "ABD"
    Category.BIST -> "BIST"
    Category.FON -> "Fon"
    Category.EMTIA -> "Emtia"
    Category.NAKIT -> "Nakit"
}

/** Miktarın yanında yazılan birim: "10 adet", "5,5 gram", "120 pay". Nakitte birim yazılmaz. */
fun UnitType.etiket(): String = when (this) {
    UnitType.ADET -> "adet"
    UnitType.PAY -> "pay"
    UnitType.GRAM -> "gram"
    UnitType.TL -> "₺"
}
