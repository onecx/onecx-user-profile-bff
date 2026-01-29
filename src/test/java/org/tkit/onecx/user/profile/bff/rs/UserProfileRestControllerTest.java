package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.user.profile.bff.rs.controllers.UserProfileRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileRestController.class)
class UserProfileRestControllerTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    static final String MOCK_ID = "MOCK";

    @BeforeEach
    void resetExpectation() {

        try {
            mockServerClient.clear(MOCK_ID);
        } catch (Exception ex) {
            //  mockId not existing
        }
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

        // dynamic mock
        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        resetExpectation();

        ProblemDetailResponseDTO problem = new ProblemDetailResponseDTO();
        problem.setErrorCode(MANUAL_ERROR);
        problem.setDetail("Manual detail of error");

        // dynamic mock
        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

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

        // dynamic mock
        UserPersonDTO personDTO = new UserPersonDTO();
        personDTO.setDisplayName("TestOrg super user");
        personDTO.setEmail("test@testOrg.com");
        personDTO.setFirstName("Superuser");
        personDTO.setLastName("TestOrgus");
        UserProfileDTO userProfileDTO = new UserProfileDTO();
        userProfileDTO.setUserId("user1");
        userProfileDTO.setOrganization("testOrg");
        userProfileDTO.setIdentityProvider("database");
        userProfileDTO.setIdentityProviderId("db");
        userProfileDTO.setPerson(personDTO);

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.GET)
                        .withHeader(APM_HEADER_PARAM, ADMIN))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(200)
                        .withBody(JsonBody.json(userProfileDTO)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

        resetExpectation();

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.GET)
                        .withHeader(APM_HEADER_PARAM, USER))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(200)
                        .withBody(JsonBody.json(userProfileDTO)));

        // standard USER can make a GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

        resetExpectation();

        ProblemDetailResponseDTO problem = new ProblemDetailResponseDTO();
        problem.setErrorCode(MANUAL_ERROR);
        problem.setDetail("Manual detail of error");

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.GET))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON) // ← CT
                        .withBody(JsonBody.json(problem)));

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

        //dynamic mock
        UserProfileDTO updated = new UserProfileDTO();
        updated.setUserId("user1");
        updated.setOrganization("testOrg");
        updated.setIdentityProvider("database");
        updated.setIdentityProviderId("db");
        updated.setPerson(new UserPersonDTO().email(updateDTO.getPerson().getEmail()));

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfile/me")
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateDTO, MatchType.ONLY_MATCHING_FIELDS)))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(updated)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);
        assertThat(response.getPerson().getEmail()).isEqualTo(updateDTO.getPerson().getEmail());
    }
}
