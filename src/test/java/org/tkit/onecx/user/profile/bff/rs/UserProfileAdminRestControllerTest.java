package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.user.profile.bff.rs.controllers.UserProfileAdminRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.clients.model.ProblemDetailResponse;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileAdminRestController.class)
class UserProfileAdminRestControllerTest extends AbstractTest {

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
    void deleteUserProfileTest() {

        // dynamic mock
        mockServerClient.when(
                request()
                        .withPath("/internal/userProfiles/user1")
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

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
                .get("/userProfiles/{id}")
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
                        .withPath("/internal/userProfiles/user1")
                        .withMethod(HttpMethod.GET))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(200)
                        .withBody(JsonBody.json(userProfileDTO)));

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
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

        response = given()
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
        assertThat(response.getPerson().getEmail()).isEqualTo("test@testOrg.com");

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

        // dynamic mock

        ProblemDetailResponse upProblem = new ProblemDetailResponse();
        upProblem.setErrorCode("CONSTRAINT_VIOLATIONS");
        upProblem.setDetail("Manual CONSTRAINT_VIOLATIONS detail");

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfiles/search")
                        .withMethod(HttpMethod.POST)
                        .withHeader(CUSTOM_FLOW_HEADER, CFH_ERROR_CONSTRAINT_VIOLATIONS))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(BAD_REQUEST.getStatusCode())
                        .withBody(JsonBody.json(upProblem)));

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
        criteriaDTO.setUserId("user");
        criteriaDTO.setEmail("*test.de");

        // dynamic mock
        UserPersonDTO personDTO = new UserPersonDTO();
        personDTO.setDisplayName("User 1");
        personDTO.setEmail("user2@test.de");
        UserProfileDTO userProfileDTO = new UserProfileDTO();
        userProfileDTO.setUserId("user1");
        userProfileDTO.setId("u1");
        userProfileDTO.setOrganization("testOrg");
        userProfileDTO.setPerson(personDTO);
        UserPersonDTO person2DTO = new UserPersonDTO();
        person2DTO.setDisplayName("User 2");
        person2DTO.setEmail("user2@test.de");
        UserProfileDTO userProfile2DTO = new UserProfileDTO();
        userProfile2DTO.setUserId("user2");
        userProfile2DTO.setId("u2");
        userProfile2DTO.setPerson(person2DTO);
        UserProfilePageResultDTO userProfilePageResultDTO = new UserProfilePageResultDTO();
        userProfilePageResultDTO.setStream(List.of(userProfileDTO, userProfile2DTO));
        userProfilePageResultDTO.setSize(2);

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfiles/search")
                        .withMethod(HttpMethod.POST)
                        .withHeader(APM_HEADER_PARAM, ADMIN)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(criteriaDTO)))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(userProfilePageResultDTO)));

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
        assertThat(response.getStream().get(1).getPerson().getEmail()).isEqualTo("user2@test.de");

        response = given()
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
        assertThat(response.getStream().get(1).getPerson().getEmail()).isEqualTo("user2@test.de");
    }

    @Test
    void updateUserProfileTest() {
        UpdateUserProfileRequestDTO update = new UpdateUserProfileRequestDTO();
        UserPersonDTO userPersonDTO = new UserPersonDTO();
        UserPersonAddressDTO address = new UserPersonAddressDTO();
        address.setStreetNo("10");
        address.setCity("Muenich");
        userPersonDTO.setAddress(address);
        userPersonDTO.setDisplayName("User 1");
        userPersonDTO.setEmail("user1@test.de");
        update.setPerson(userPersonDTO);
        update.setModificationCount(2);

        // Test without apm
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(new UpdateUserProfileRequestDTO())
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
                .body(new UpdateUserProfileRequestDTO())
                .put("/user3")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(new UpdateUserProfileRequestDTO())
                .put("/user4")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");

        //dynamic mock
        UserProfileDTO updated = new UserProfileDTO();
        updated.setUserId("user1");
        updated.setOrganization("testOrg");
        updated.setIdentityProvider("database");
        updated.setIdentityProviderId("db");
        updated.setPerson(new UserPersonDTO().email(update.getPerson().getEmail()));

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfiles/user1")
                        .withMethod(HttpMethod.PUT)
        //                        .withBody(JsonBody.json(update, MatchType.ONLY_MATCHING_FIELDS))
        )
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(updated)));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/user1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        // standard USER will get FORBIDDEN without WRITE permission
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/user1")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        //dynamic mock
        ProblemDetailResponseDTO violations = new ProblemDetailResponseDTO();
        violations.setErrorCode("OPTIMISTIC_LOCK");
        violations.setDetail("Manual OPTIMISTIC_LOCK detail of error");

        mockServerClient.when(
                request()
                        .withPath("/internal/userProfiles/user5")
                        .withMethod(HttpMethod.PUT)
        ).withId(MOCK_ID).respond(req -> response()
                .withStatusCode(400)
                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                .withBody(JsonBody.json(violations)));

        // opt lock test
        error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/user5")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo("OPTIMISTIC_LOCK");
    }
}
