package com.flightmonitor.smoke;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("smoke")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.boot.admin.client.enabled=false"
})
class FlightSearchSmokeTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void actuatorHealth_returns200Up() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void actuatorInfo_returns200() {
        given()
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(200);
    }

    @Test
    void swaggerUi_returns200() {
        given()
                .redirects().follow(true)
                .when()
                .get("/swagger-ui.html")
                .then()
                .statusCode(200);
    }

    @Test
    void apiDocs_returns200WithOpenApiJson() {
        given()
                .when()
                .get("/api-docs")
                .then()
                .statusCode(200)
                .body("openapi", notNullValue());
    }

    @Test
    void flightSearch_withValidParams_returns200() {
        String futureDate = LocalDate.now().plusDays(10).toString();
        given()
                .param("origin", "BSB")
                .param("destination", "LIS")
                .param("departureDate", futureDate)
                .when()
                .get("/api/v1/flights/search")
                .then()
                .statusCode(200);
    }
}
