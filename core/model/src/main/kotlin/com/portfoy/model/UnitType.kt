package com.portfoy.model

/**
 * Varlığın ölçü birimi: hisse için adet, fon için pay, emtia için gram, nakit için TL.
 * Dövizde miktar para biriminin kendisidir ([BIRIM]); ekranda varlığın para birimi yazılır ("1.000,00 USD").
 */
enum class UnitType { ADET, PAY, GRAM, BIRIM, TL }
