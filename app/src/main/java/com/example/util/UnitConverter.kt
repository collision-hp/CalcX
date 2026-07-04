package com.example.util

object UnitConverter {
    enum class Category {
        CURRENCY, LENGTH, WEIGHT, TEMPERATURE
    }

    data class UnitOption(val code: String, val fullName: String)

    val unitsByCategory = mapOf(
        Category.CURRENCY to listOf(
            UnitOption("USD", "US Dollar"),
            UnitOption("INR", "Indian Rupee"),
            UnitOption("EUR", "Euro"),
            UnitOption("GBP", "British Pound"),
            UnitOption("JPY", "Japanese Yen")
        ),
        Category.LENGTH to listOf(
            UnitOption("m", "Meter"),
            UnitOption("km", "Kilometer"),
            UnitOption("cm", "Centimeter"),
            UnitOption("mile", "Mile"),
            UnitOption("foot", "Foot"),
            UnitOption("inch", "Inch")
        ),
        Category.WEIGHT to listOf(
            UnitOption("kg", "Kilogram"),
            UnitOption("g", "Gram"),
            UnitOption("lb", "Pound"),
            UnitOption("oz", "Ounce")
        ),
        Category.TEMPERATURE to listOf(
            UnitOption("°C", "Celsius"),
            UnitOption("°F", "Fahrenheit"),
            UnitOption("K", "Kelvin")
        )
    )

    private val currencyRates = mapOf(
        "USD" to 1.0,
        "INR" to 83.12,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 155.40
    )

    private val lengthRates = mapOf(
        "m" to 1.0,
        "km" to 1000.0,
        "cm" to 0.01,
        "mile" to 1609.344,
        "foot" to 0.3048,
        "inch" to 0.0254
    )

    private val weightRates = mapOf(
        "kg" to 1.0,
        "g" to 0.001,
        "lb" to 0.45359237,
        "oz" to 0.0283495231
    )

    fun convert(value: Double, fromUnit: String, toUnit: String, category: Category): Double {
        if (fromUnit == toUnit) return value

        return when (category) {
            Category.CURRENCY -> {
                val fromRate = currencyRates[fromUnit] ?: 1.0
                val toRate = currencyRates[toUnit] ?: 1.0
                (value / fromRate) * toRate
            }
            Category.LENGTH -> {
                val fromRate = lengthRates[fromUnit] ?: 1.0
                val toRate = lengthRates[toUnit] ?: 1.0
                (value * fromRate) / toRate
            }
            Category.WEIGHT -> {
                val fromRate = weightRates[fromUnit] ?: 1.0
                val toRate = weightRates[toUnit] ?: 1.0
                (value * fromRate) / toRate
            }
            Category.TEMPERATURE -> {
                val celsius = when (fromUnit) {
                    "°C" -> value
                    "°F" -> (value - 32.0) / 1.8
                    "K" -> value - 273.15
                    else -> value
                }
                when (toUnit) {
                    "°C" -> celsius
                    "°F" -> celsius * 1.8 + 32.0
                    "K" -> celsius + 273.15
                    else -> celsius
                }
            }
        }
    }
}
