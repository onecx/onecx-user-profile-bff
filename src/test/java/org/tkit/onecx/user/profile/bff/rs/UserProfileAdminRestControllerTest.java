package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.user.profile.bff.rs.controllers.UserProfileAdminRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileAdminRestController.class)
class UserProfileAdminRestControllerTest extends AbstractTest {

    @Test
    void deleteUserProfileTest() {
        // Test with header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .delete("/{id}")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // standard USER with READ permission will get FORBIDDEN
        given()
                .when()
                .pathParam("id", "user1")
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .delete("/{id}")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        // Test without header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .delete("/{id}")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void getUserProfileTest() {
        // Test without header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // found for user1
        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .pathParam("id", "user1")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("cap@capgemini.com");

        // standard USER with READ will also get the response
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .pathParam("id", "user1")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("cap@capgemini.com");

        // not found for user 2
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .pathParam("id", "user2")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void searchUserProfileErrorTest() {
        // Test without apm
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(new UserPersonCriteriaDTO())
                .post("/search")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // without criteria
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .post("/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON);

        // with empty criteria
        var error = given()
                .when()
                .contentType(APPLICATION_JSON)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_CONSTRAINT_VIOLATIONS)
                .body(new UserPersonCriteriaDTO())
                .post("/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");

    }

    @Test
    void searchUserProfileTest() {
        UserPersonCriteriaDTO criteriaDTO = new UserPersonCriteriaDTO();
        criteriaDTO.setUserId("user1");
        criteriaDTO.setEmail("*cap.de");
        var response = given()
                .when()
                .contentType(APPLICATION_JSON)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .body(criteriaDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserProfilePageResultDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getStream().get(1).getPerson().getEmail()).isEqualTo("user2@cap.de");

        // search also possible with standard USER with only READ permission
        response = given()
                .when()
                .contentType(APPLICATION_JSON)
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .body(criteriaDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserProfilePageResultDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getStream().get(1).getPerson().getEmail()).isEqualTo("user2@cap.de");
    }

    @Test
    void updateUserProfileTest() {
        UpdateUserPersonRequestDTO update = new UpdateUserPersonRequestDTO();
        UserPersonAddressDTO address = new UserPersonAddressDTO();
        address.setStreetNo("10");
        address.setCity("Muenich");
        update.setAddress(address);
        update.setDisplayName("User 1");
        update.setEmail("user1@cap.de");
        update.setModificationCount(2);

        // Test without apm
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(new UpdateUserPersonRequestDTO())
                .put("/user1")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/user2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user3")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user4")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // standard USER will get FORBIDDEN without WRITE permission
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        // opt lock test
        error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user5")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo("OPTIMISTIC_LOCK");
    }
}
