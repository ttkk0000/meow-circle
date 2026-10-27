package com.ttkk0000.meowcircle.client

data class MainlandProvince(val code: String, val name: String)
data class MainlandCity(val code: String, val provinceCode: String, val name: String, val value: String,
    val parentName: String, val pinyin: String, val latitude: Double, val longitude: Double, val alias: String) {
    val provinceName: String get() = mainlandProvinces.first { it.code == provinceCode }.name
    val description: String get() = listOf(provinceName, parentName).filter { it.isNotBlank() }.distinct().joinToString(" · ")
}

val mainlandCities: List<MainlandCity> by lazy {
    mainlandCityRows.flatMap { block -> block.lineSequence().map { line ->
        val p = line.split('|')
        MainlandCity(p[0], p[1], p[2], p[3], p[4], p[5], p[6].toDouble(), p[7].toDouble(), p[8])
    }.toList() }
}

private val mainlandCityIndex: Map<String, MainlandCity> by lazy {
    buildMap {
        mainlandCities.forEach { city -> put(city.code, city); put(city.name, city); put(city.value, city) }
        mainlandCities.groupBy { it.alias }.forEach { (alias, cities) ->
            if (cities.size == 1 && !containsKey(alias)) put(alias, cities.single())
        }
    }
}

fun mainlandCity(value: String): MainlandCity? = mainlandCityIndex[value.trim()]
fun normalizeCity(value: String): String = mainlandCity(value)?.value ?: value.trim()
fun sameCity(a: String, b: String): Boolean = normalizeCity(a) == normalizeCity(b)
fun searchMainlandCities(query: String = "", provinceCode: String = ""): List<MainlandCity> {
    val term = query.trim().lowercase().replace(" ", "")
    return mainlandCities.filter { city -> (provinceCode.isBlank() || city.provinceCode == provinceCode) &&
        (term.isBlank() || listOf(city.name, city.value, city.alias, city.pinyin, city.code, city.provinceName, city.parentName).any { term in it.lowercase() }) }
}
