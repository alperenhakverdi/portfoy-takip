package com.portfoy.model

/**
 * İşlem türü. [AZALTMA], M17 ile eklendi: bir varlığın adedini azaltır (satış gibi ama
 * gerçekleşen kâr/zarar hesaplanmaz — yalnızca adet ve toplam maliyet, mevcut ağırlıklı ortalama
 * maliyet korunarak orantılı düşürülür). Bkz. [com.portfoy.calc.positionOf].
 */
enum class TransactionType { ALIS, AZALTMA }
