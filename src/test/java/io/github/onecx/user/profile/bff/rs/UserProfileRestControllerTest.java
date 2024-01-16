package io.github.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;

import java.util.ArrayList;

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
import io.github.onecx.user.profile.bff.rs.controllers.UserProfileRestController;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserProfileRestController.class)
class UserProfileRestControllerTest extends AbstractTest {

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
    void createUserPreferenceTest() {
        CreateUserPreferenceDTO cupDTO = new CreateUserPreferenceDTO();
        cupDTO.setApplicationId("app1");
        cupDTO.setName("name1");
        cupDTO.setValue("value1");
        cupDTO.setDescription("desc1");
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        UserPreference userPreference = new UserPreference();
        userPreference.setApplicationId(cupDTO.getApplicationId());
        userPreference.setName(cupDTO.getName());
        userPreference.setDescription(cupDTO.getDescription());
        userPreference.setValue(cupDTO.getValue());
        userPreference.setId("id1");
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences")
                .withMethod(HttpMethod.POST))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(userPreference)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract().as(UserPreferenceDTO.class);
        assertThat(response.getId()).isEqualTo(userPreference.getId());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences")
                .withMethod(HttpMethod.POST))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(cupDTO)
                .post("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void deleteMyUserProfile() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        mockServerClient.when(request().withPath("/v1/userProfile/me")
                .withMethod(HttpMethod.DELETE))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me")
                .withMethod(HttpMethod.DELETE))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void deleteUserPreference() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences/pref1")
                .withMethod(HttpMethod.DELETE))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences/pref1")
                .withMethod(HttpMethod.DELETE))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .delete("/preferences/pref1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void getMyUserProfile() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

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
        mockServerClient.when(request().withPath("/v1/userProfile/me")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(userProfile)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userProfile.getUserId());
        assertThat(response.getPerson().getEmail()).isEqualTo(userProfile.getPerson().getEmail());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void getUserPerson() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var person = new UserPerson();
        person.setDisplayName("Capgemini super user");
        person.setEmail("cap@capgemini.com");
        person.setFirstName("Superuser");
        person.setLastName("Capgeminius");
        UserPersonAddress addresss = new UserPersonAddress();
        addresss.setStreet("Obergasse");
        person.setAddress(addresss);
        mockServerClient.when(request().withPath("/v1/userProfile/me/person")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(person)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPersonDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(person.getEmail());
        assertThat(response.getAddress().getStreet()).isEqualTo(person.getAddress().getStreet());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/person")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/person")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void getUserPreference() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var preferences = new UserPreferences();
        preferences.setPreferences(new ArrayList<>());
        UserPreference preference1 = new UserPreference();
        preference1.setId("1");
        preference1.setName("name1");
        preference1.setDescription("desc1");
        preference1.setApplicationId("app1");
        preference1.setValue("value1");
        preferences.getPreferences().add(preference1);
        UserPreference preference2 = new UserPreference();
        preference2.setId("2");
        preference2.setName("name2");
        preference2.setDescription("desc2");
        preference2.setApplicationId("app2");
        preference2.setValue("value2");
        preferences.getPreferences().add(preference2);
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(preferences)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPreferencesDTO.class);

        assertThat(response.getPreferences()).hasSize(2);

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/preferences")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void getUserSettings() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var settings = new UserProfileAccountSettings();
        settings.setMenuMode(MenuMode.STATIC);
        settings.setHideMyProfile(false);
        settings.setColorScheme(ColorScheme.DARK);
        settings.setTimezone("get/muenich");

        mockServerClient.when(request().withPath("/v1/userProfile/me/settings")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(settings)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileAccountSettingsDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getHideMyProfile()).isEqualTo(settings.getHideMyProfile());
        assertThat(response.getMenuMode()).hasToString(settings.getMenuMode().toString());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/settings")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get("/settings")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void updateUserPerson() {
        var update = new UpdateUserPersonRequestDTO();
        update.setEmail("test@email.de");

        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var person = new UserPerson();
        person.setDisplayName("Capgemini super user");
        person.setEmail("test@email.de");
        person.setFirstName("Superuser");
        person.setLastName("Capgeminius");
        UserPersonAddress addresss = new UserPersonAddress();
        addresss.setStreet("Obergasse");
        person.setAddress(addresss);
        mockServerClient.when(request().withPath("/v1/userProfile/me/person")
                .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(person)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .body(update)
                .contentType(APPLICATION_JSON)
                .put("/person")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPersonDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(person.getEmail());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/person")
                .withMethod(HttpMethod.PUT))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/person")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
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
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        UserPreference preference1 = new UserPreference();
        preference1.setId("1");
        preference1.setName("name1");
        preference1.setDescription("desc1");
        preference1.setApplicationId("app1");
        preference1.setValue(update);
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences/pref1")
                .withMethod(HttpMethod.PATCH))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(preference1)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserPreferenceDTO.class);

        assertThat(response.getValue()).isEqualTo(update);

        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences/pref2")
                .withMethod(HttpMethod.PATCH))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));
        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/preferences/pref1")
                .withMethod(HttpMethod.PATCH))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .patch("/preferences/pref1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void updateUserSettings() {
        var update = new UpdateUserSettingsDTO();
        update.setColorScheme(ColorSchemeDTO.LIGHT);
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .body(update)
                .get("/settings")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var settings = new UserProfileAccountSettings();
        settings.setMenuMode(MenuMode.STATIC);
        settings.setHideMyProfile(false);
        settings.setColorScheme(ColorScheme.LIGHT);
        settings.setTimezone("get/muenich");

        mockServerClient.when(request().withPath("/v1/userProfile/me/settings")
                .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(settings)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(UserProfileAccountSettingsDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getHideMyProfile()).isEqualTo(settings.getHideMyProfile());
        assertThat(response.getMenuMode()).hasToString(settings.getMenuMode().toString());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/settings")
                .withMethod(HttpMethod.PUT))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());

    }

    @Test
    void testConstraintViolationException() {
        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
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
        mockServerClient.when(request().withPath("/v1/userProfile/me/settings")
                .withMethod(HttpMethod.PUT))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/settings");

        response.then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        assertThat(response.getBody().prettyPrint()).isEmpty();

    }
}
