package com.flightmonitor.smoke;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@Tag("smoke")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.boot.admin.client.enabled=false"
})
class AlertSmokeTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void getAlerts_byEmail_returns200WithList() {
        given()
                .param("email", "smoke@test.com")
                .when()
                .get("/api/v1/alerts")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void createAlert_thenDelete_returns201And204() {
        String body = """
                {
                    "userEmail": "smoke@test.com",
                    "origin": "BSB",
                    "destination": "LIS",
                    "departureDate": "%s",
                    "targetPrice": 299.99
                }
                """.formatted(LocalDate.now().plusDays(10));

        String location = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/alerts")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .extract().header("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);

        given()
                .when()
                .delete("/api/v1/alerts/" + id)
                .then()
                .statusCode(204);
    }
}
