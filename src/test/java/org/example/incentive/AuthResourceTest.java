package org.example.incentive;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class AuthResourceTest {

    @Test
    public void testRegisterEndpoint() {
        String requestBody = """
            {
                "name": "Test User",
                "email": "test@test.com",
                "password": "password123",
                "phone": "11999999999"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/auth/register")
            .then()
            .statusCode(201)
            .body("token", notNullValue())
            .body("user.email", notNullValue());
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        String requestBody = """
            {
                "email": "invalid@test.com",
                "password": "wrongpassword"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(401);
    }
}
