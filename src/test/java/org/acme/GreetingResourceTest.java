package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class GreetingResourceTest {

    @Test
    public void testUserEndpoint() {
        given()
          .when().post("/user/admin/drop-and-create")
          .then()
             .statusCode(204);

        given()
        .when().post("/user/admin/test-data?n=1")
        .then()
           .statusCode(200)
           .body(containsString("Inserted 1 record(s)"));

        
        given()
        .when().get("/user/random")
        .then()
           .statusCode(200)
           .body(containsString("\"id\":1"));
    }
}
