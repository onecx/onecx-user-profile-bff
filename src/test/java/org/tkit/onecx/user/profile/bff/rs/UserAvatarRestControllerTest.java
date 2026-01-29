package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.user.profile.bff.rs.controllers.UserAvatarRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.clients.model.*;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserAvatarRestController.class)
class UserAvatarRestControllerTest extends AbstractTest {

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
    void deleteUserAvatarTest() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // user with no DELETE permission will get FORBIDDEN
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        mockServerClient.when(
                request()
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

        ProblemDetailResponse delProblem = new ProblemDetailResponse();
        delProblem.setErrorCode("MANUAL_ERROR");
        delProblem.setDetail("Manual detail of error");

        mockServerClient.when(
                request()
                        .withMethod(HttpMethod.DELETE)
                        .withHeader(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(delProblem)));

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

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void getUserAvatarTest() throws IOException {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        byte[] bytes = Files.readAllBytes(avatar.toPath());
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET)
                .withQueryStringParameter("refType", "medium"))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.JPEG)
                        .withBody(bytes));

        var avatarByteArray = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get()
                .then()
                .contentType("image/jpeg")
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        // standard USER with READ permission is enough
        avatarByteArray = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .get()
                .then()
                .contentType(ContentType.IMAGE_JPEG.getMimeType())
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        resetExpectation();
        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET)
                .withQueryStringParameter("refType", "medium"))
                .withPriority(200)
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .log().all()
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());

        resetExpectation();
        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET))
                .withId(MOCK_ID)
                .withPriority(300)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .queryParam("refType", RefTypeDTO.LARGE)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getUserAvatarBadRequestTest() throws IOException {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        byte[] bytes = Files.readAllBytes(avatar.toPath());
        // do not send content type and dont send image
        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET)
                .withQueryStringParameter("refType", "medium"))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode()));

        given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        // do not send content type and send image
        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET)
                .withQueryStringParameter("refType", "medium"))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withBody(bytes));

        given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        // do not set body but send content type
        resetExpectation();
        mockServerClient.when(request().withPath("/internal/avatar/me")
                .withMethod(HttpMethod.GET)
                .withQueryStringParameter("refType", "medium"))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.JPEG));
        given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void uploadAvatarTest() {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        given()
                .when()
                .contentType("image/jpg")
                .body(avatar)
                .post()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // USER with no WRITE permission will get FORBIDDEN
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType("image/jpg")
                .body(avatar)
                .post()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        // dynamic mock
        mockServerClient.when(
                request()
                        .withMethod(HttpMethod.POST)
                        .withQueryStringParameter("refType", "medium"))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        var response = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType("image/jpg")
                .body(avatar)
                .post()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();

        // dynamic mock
        ProblemDetailResponse upProblem = new ProblemDetailResponse();
        upProblem.setErrorCode("MANUAL_ERROR");
        upProblem.setDetail("Manual detail of error");

        resetExpectation();

        mockServerClient.when(
                request()
                        .withMethod(HttpMethod.POST)
                        .withQueryStringParameter("refType", "small")
                        .withHeader(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(upProblem)));

        var error = given()
                .when()
                .queryParam("refType", RefTypeDTO.SMALL)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType("image/jpg")
                .body(avatar)
                .post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void testBadRequestWithoutProblemResponse() {

        // dynamic mock
        mockServerClient.when(
                request()
                        .withMethod(HttpMethod.GET)
                        .withQueryStringParameter("refType", "medium")
                        .withHeader(CUSTOM_FLOW_HEADER, CFH_ERROR_NO_CONTENT))
                .withId(MOCK_ID).respond(req -> response()
                        .withStatusCode(BAD_REQUEST.getStatusCode()));

        var response = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_NO_CONTENT)
                .contentType(APPLICATION_JSON)
                .get();

        response.then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        assertThat(response.getBody().prettyPrint()).isEmpty();
    }

}
