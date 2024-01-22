package io.github.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.quarkus.log.cdi.LogService;

import gen.io.github.onecx.user.profile.bff.clients.model.*;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.*;
import io.github.onecx.user.profile.bff.rs.controllers.UserProfileAdminRestController;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileAdminRestController.class)
class UserProfileAdminRestControllerTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @BeforeEach
    void resetMockServer() {
        mockServerClient.reset();
        // throw 500 if the apm-principal token is not there
        mockServerClient.when(request().withHeader(not(APM_HEADER_PARAM), string(".*")))
                .withPriority(999)
                .respond(httpRequest -> response().withStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

    }

    @Test
    void deleteUserProfileTest() {
        mockServerClient.when(request().withPath("/internal/userProfiles/user1")
                .withMethod(HttpMethod.DELETE))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        // Test with header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .delete("/{id}")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // Test without header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .delete("/{id}")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void getUserProfileTest() {
        // user2 not found
        mockServerClient.when(request().withPath("/internal/userProfiles/user2")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        // user1 exists
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId("user1");
        userProfile.setOrganization("capgemini");
        userProfile.setIdentityProvider("database");
        userProfile.setIdentityProviderId("db");
        var person = new UserPerson();
        userProfile.setPerson(person);
        person.setDisplayName("Capgemini super user");
        person.setEmail("cap@capgemini.com");
        person.setFirstName("Superuser");
        person.setLastName("Capgeminius");
        mockServerClient.when(request().withPath("/internal/userProfiles/user1")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(userProfile)));

        // Test without header apm-principal-token
        given()
                .when()
                .pathParam("id", "user1")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        // found for user1
        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .pathParam("id", "user1")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userProfile.getUserId());
        assertThat(response.getPerson().getEmail()).isEqualTo(userProfile.getPerson().getEmail());

        // not found for user 2
        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user2", null))
                .pathParam("id", "user2")
                .get("/{id}")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void searchUserProfileErrorTest() {
        UserPersonCriteria criteria = new UserPersonCriteria();
        ProblemDetailResponse problemDetailResponse = new ProblemDetailResponse();
        problemDetailResponse.setErrorCode("CONSTRAINT_VIOLATIONS");
        problemDetailResponse.setDetail("searchUserProfile.userPersonCriteriaDTO: must not be null");
        mockServerClient.when(request().withPath("/internal/userProfiles/search").withBody(JsonBody.json(criteria)))
                .withPriority(999)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problemDetailResponse)));

        // Test without apm
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(new UserPersonCriteriaDTO())
                .post("/search")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        // without criteria
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .post("/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON);

        // with empty criteria
        var error = given()
                .when()
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .body(new UserPersonCriteriaDTO())
                .post("/search")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo(problemDetailResponse.getErrorCode());

    }

    @Test
    void searchUserProfileTest() {
        UserPersonCriteria criteria = new UserPersonCriteria();
        criteria.setUserId("user1");
        criteria.setEmail("*cap.de");
        UserProfilePageResult result = new UserProfilePageResult();
        result.setSize(2);
        UserProfile up1 = new UserProfile();
        up1.setUserId("user1");
        up1.setId("u1");
        UserProfileAccountSettings as1 = new UserProfileAccountSettings();
        as1.setColorScheme(ColorScheme.AUTO);
        as1.setMenuMode(MenuMode.SLIMPLUS);
        up1.setAccountSettings(as1);
        UserPerson person1 = new UserPerson();
        person1.setDisplayName("User 1");
        person1.setEmail("user1@cap.de");
        up1.setPerson(person1);

        UserProfile up2 = new UserProfile();
        up2.setUserId("user1");
        up2.setId("u1");
        UserProfileAccountSettings as2 = new UserProfileAccountSettings();
        as2.setColorScheme(ColorScheme.AUTO);
        as2.setMenuMode(MenuMode.SLIMPLUS);
        up2.setAccountSettings(as2);
        UserPerson person2 = new UserPerson();
        person2.setDisplayName("User 1");
        person2.setEmail("user1@cap.de");
        up2.setPerson(person2);

        List<UserProfile> profileList = Arrays.asList(up1, up2);
        result.setStream(profileList);
        mockServerClient.when(request().withPath("/internal/userProfiles/search").withBody(JsonBody.json(criteria)))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(result)));
        UserPersonCriteriaDTO criteriaDTO = new UserPersonCriteriaDTO();
        criteriaDTO.setUserId("user1");
        criteriaDTO.setEmail("*cap.de");
        var response = given()
                .when()
                .contentType(APPLICATION_JSON)
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .body(criteriaDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserProfilePageResultDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getStream().get(1).getPerson().getEmail()).isEqualTo(person2.getEmail());
    }

    @Test
    void updateUserProfileTest() {
        mockServerClient.when(request().withPath("/internal/userProfiles/user2"))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));
        mockServerClient.when(request().withPath("/internal/userProfiles/user3"))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));
        ProblemDetailResponse problemDetailResponse = new ProblemDetailResponse();
        problemDetailResponse.setErrorCode("CONSTRAINT_VIOLATIONS");
        problemDetailResponse.setDetail("searchUserProfile.userPersonCriteriaDTO: must not be null");
        mockServerClient.when(request().withPath("/internal/userProfiles/user4"))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problemDetailResponse)));
        mockServerClient.when(request().withPath("/internal/userProfiles/user1"))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

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
                .body(new UserPersonCriteriaDTO())
                .put("/user1")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user2", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/user2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user3", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user3")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user4", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user4")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo(problemDetailResponse.getErrorCode());

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // opt lock test
        problemDetailResponse.setErrorCode("OPTIMISTIC_LOCK");
        mockServerClient.when(request().withPath("/internal/userProfiles/user5"))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problemDetailResponse)));

        error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user5", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .body(new UserPersonCriteriaDTO())
                .put("/user5")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo(problemDetailResponse.getErrorCode());
    }
}
