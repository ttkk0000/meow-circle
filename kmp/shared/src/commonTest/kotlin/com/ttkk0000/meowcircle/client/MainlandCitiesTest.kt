package com.ttkk0000.meowcircle.client

import kotlin.test.*

class MainlandCitiesTest {
    @Test fun aliasesKeepCountyCitiesAndPrefecturesDistinct() {
        assertTrue(sameCity("武汉", "武汉市"))
        assertFalse(sameCity("恩施市", "恩施土家族苗族自治州"))
        assertFalse(sameCity("苏州", "宿州"))
        assertEquals("659001", mainlandCity("石河子市")?.code)
        assertEquals("延吉", searchMainlandCities("yanji").single().value)
    }
    @Test fun viewportNeverBecomesAnEventPoint() {
        assertEquals("country-center", mapCityCenter("").id)
        assertEquals("country-center", mapCityCenter("未知地区").id)
        assertEquals(mainlandCity("拉萨")!!.latitude, mapCityCenter("拉萨").latitude)
        assertEquals(31, mainlandProvinces.size)
        assertEquals(694, mainlandCities.count { it.name.endsWith("市") })
    }
}
