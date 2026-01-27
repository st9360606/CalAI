package com.calai.bitecal.core.health

/** 從伺服器/本機的字串性別對應到計算用 Gender。
 * 規則：只有 "MALE" 視為男性，其餘（含 FEMALE/OTHER/null）一律視為女性。
 */
fun toCalcGender(genderStr: String?): Gender =
    if (genderStr?.equals("MALE", ignoreCase = true) == true) Gender.Male else Gender.Female
