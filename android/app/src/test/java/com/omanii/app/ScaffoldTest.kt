package com.omanii.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaffoldTest {
    @Test
    fun launchActivityUsesTheApplicationNamespace() {
        assertEquals("com.omanii.app", MainActivity::class.java.packageName)
    }
}
