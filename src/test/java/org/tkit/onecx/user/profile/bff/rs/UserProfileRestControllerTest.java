package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.user.profile.bff.rs.controllers.UserProfileRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileRestController.class)
class UserProfileRestControllerTest extends AbstractTest {

    @Test
    void deleteMyUserProfile() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // standard USER get FORBIDDEN on delete with only READ permission
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
    }

    @Test
    void getMyUserProfile() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

        // standard USER can make a GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
    }

    @Test
    void testConstraintViolationException() {
        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");
    }

    @Test
    void updateMyUserProfile() {
        UpdateUserProfileRequestDTO updateDTO = new UpdateUserProfileRequestDTO();
        updateDTO.setModificationCount(0);
        updateDTO.setPerson(new UserPersonDTO().email("test@testOrg.com"));
        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);
        assertThat(response.getPerson().getEmail()).isEqualTo(updateDTO.getPerson().getEmail());
    }
}
