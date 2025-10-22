package com.calai.app.data.fasting.model

enum class FastingPlan(val code: String, val eatingHours: Int) {
    P14_10("14:10", 10),
    P16_8("16:8", 8),
    P20_4("20:4", 4),
    P22_2("22:2", 2),
    P12_12("12:12", 12),
    P18_6("18:6", 6);
    companion object { fun of(code: String) = entries.first { it.code == code } }
}
