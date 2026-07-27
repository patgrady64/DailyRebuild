package com.pgdevhouse.dailyrebuild.domain

fun normalizeFoodBarcode(raw: String): String {
    return raw.filter(Char::isDigit)
}

fun isSupportedFoodBarcode(barcode: String): Boolean {
    return barcode.length == 8 || barcode.length == 12 || barcode.length == 13
}
