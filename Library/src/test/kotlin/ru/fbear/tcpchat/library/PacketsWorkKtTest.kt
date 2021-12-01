package ru.fbear.tcpchat.library

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PacketsWorkKtTest {

    @Test
    fun toLong() {
        val unixTime = 1638133137L
        val byteArrayOfUnixTime = unixTime.toByteArray()

        assertEquals(unixTime, byteArrayOfUnixTime.toLong())
    }

    @Test
    fun toByteArray() {
        val unixTime = 1638133137L

        val expected = byteArrayOf(145.toByte(), 237.toByte(), 163.toByte(), 97.toByte())

        assertArrayEquals(expected, unixTime.toByteArray())

    }

    @Test
    fun countBitsTest() {
        val unixTime = 1638133137L

        val expected = 31

        assertEquals(expected, unixTime.countBits())
    }
}