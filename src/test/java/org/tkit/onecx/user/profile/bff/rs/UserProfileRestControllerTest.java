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
    void createUserPreferenceTest() {
        CreateUserPreferenceDTO cupDTO = new CreateUserPreferenceDTO();
        cupDTO.setApplicationId("app1");
        cupDTO.setName("name1");
        cupDTO.setValue("value1");
        cupDTO.setDescription("desc1");

        // DO NOT send APM token and oauth
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // DO NOT send APM token
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract().as(UserPreferenceDTO.class);
        assertThat(response.getId()).isEqualTo("id1");

        // standard USER get FORBIDDEN with only READ permission
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
    }

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
    void deleteUserPreference() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // FORBIDDEN for standard USER without needed permission
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
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
    void getUserPerson() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPersonDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@testOrg.com");
        assertThat(response.getAddress().getStreet()).isEqualTo("Obergasse");

        // standard USER can make a GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPersonDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@testOrg.com");
        assertThat(response.getAddress().getStreet()).isEqualTo("Obergasse");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);
    }

    @Test
    void getUserPreference() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPreferencesDTO.class);

        assertThat(response.getPreferences()).hasSize(2);

        // standard USER can make a GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPreferencesDTO.class);

        assertThat(response.getPreferences()).hasSize(2);

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);
    }

    @Test
    void getUserSettings() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileAccountSettingsDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getHideMyProfile()).isEqualTo(Boolean.FALSE);
        assertThat(response.getMenuMode()).hasToString("STATIC");

        // standard USER can make a GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileAccountSettingsDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getHideMyProfile()).isEqualTo(Boolean.FALSE);
        assertThat(response.getMenuMode()).hasToString("STATIC");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);
    }

    @Test
    void updateUserPerson() {
        var update = new UpdateUserPersonRequestDTO();
        update.setEmail("test@testOrg.com");

        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // standard USER will get FORBIDDEN
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .body(update)
                .contentType(APPLICATION_JSON)
                .put("/person")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPersonDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@testOrg.com");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);

        error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_OPT_LOCK)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("OPTIMISTIC_LOCK");
    }

    @Test
    void updateUserPreference() {
        var update = "newValue1";
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // standard USER has FORBIDDEN for PATCH
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPreferenceDTO.class);

        assertThat(response.getValue()).isEqualTo(update);

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);
    }

    @Test
    void updateUserSettings() {
        var update = new UpdateUserSettingsDTO();
        update.setColorScheme(ColorSchemeDTO.LIGHT);
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // standard USER will get FORBIDDEN for PUT
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileAccountSettingsDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getHideMyProfile()).isEqualTo(Boolean.FALSE);
        assertThat(response.getMenuMode()).hasToString("STATIC");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(MANUAL_ERROR);
        assertThat(error.getDetail()).isEqualTo(MANUAL_ERROR_DETAIL);

        error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_OPT_LOCK)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(OPTIMISTIC_LOCK);
    }

    @Test
    void testConstraintViolationException() {
        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");
    }

    @Test
    void testBadRequestWithoutProblemResponse() {
        var update = new UpdateUserSettingsDTO();

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_NO_CONTENT)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings");

        response.then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        assertThat(response.getBody().prettyPrint()).isEmpty();

    }
}
