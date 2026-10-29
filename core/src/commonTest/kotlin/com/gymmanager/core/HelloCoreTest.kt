package com.gymmanager.core

import kotlin.test.Test
import kotlin.test.assertEquals

class HelloCoreTest {
    @Test
    fun greetingIsStable() {
        assertEquals("GymManager core", HelloCore.greeting)
    }
}
