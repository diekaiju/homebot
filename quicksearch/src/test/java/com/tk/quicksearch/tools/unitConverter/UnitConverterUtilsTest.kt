package com.tk.quicksearch.tools.unitConverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UnitConverterUtilsTest {

    @Test
    fun convertsLargeNumbersCorrectly() {
        val result = UnitConverterUtils.convertQuery("10000 metres to miles")
        assertEquals("6.21 miles (mi)", result)
    }

    @Test
    fun trimsTrailingZerosToAtMostTwoDecimals() {
        val result = UnitConverterUtils.convertQuery("1500 meters to kilometers")
        assertEquals("1.5 kilometres (km)", result)
    }

    @Test
    fun convertsTemperatureCelsiusToFahrenheit() {
        val result = UnitConverterUtils.convertQuery("100 celsius to fahrenheit")
        assertEquals("212 \u00B0F", result)
    }

    @Test
    fun convertsTemperatureFahrenheitToCelsius() {
        val result = UnitConverterUtils.convertQuery("32 f to c")
        assertEquals("0 \u00B0C", result)
    }

    @Test
    fun supportsNoSpaceTemperatureInput() {
        val result = UnitConverterUtils.convertQuery("100c to f")
        assertNotNull(result)
        assertEquals("212 \u00B0F", result)
    }

    @Test
    fun supportsKmphAlias() {
        val result = UnitConverterUtils.convertQuery("60 kmph to m/s")
        assertEquals("16.67 metres per second (m/s)", result)
    }
}
