package com.example.moduleone;

public class Greeting {

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        return "Hello, " + name + "!";
    }
}
