package com.example.moduletwo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void add_returnsSum() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    void subtract_returnsDifference() {
        assertEquals(1, calculator.subtract(3, 2));
    }

    @Test
    void multiply_returnsProduct() {
        assertEquals(6, calculator.multiply(2, 3));
    }

    @Test
    void divide_returnsQuotient() {
        assertEquals(2.5, calculator.divide(5, 2));
    }

    @Test
    void divide_throwsOnDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(1, 0));
    }
}
