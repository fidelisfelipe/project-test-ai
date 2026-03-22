package com.example.moduleone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreetingTest {

    private final Greeting greeting = new Greeting();

    @Test
    void greet_returnsExpectedMessage() {
        assertEquals("Hello, World!", greeting.greet("World"));
    }

    @Test
    void greet_throwsOnBlankName() {
        assertThrows(IllegalArgumentException.class, () -> greeting.greet(""));
    }

    @Test
    void greet_throwsOnNullName() {
        assertThrows(IllegalArgumentException.class, () -> greeting.greet(null));
    }
}
