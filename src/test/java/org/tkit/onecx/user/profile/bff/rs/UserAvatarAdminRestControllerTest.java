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
import org.tkit.onecx.user.profile.bff.rs.controllers.UserAvatarAdminRestController;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.clients.model.ProblemDetailResponse;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ImageInfoDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.RefTypeDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserAvatarAdminRestController.class)
class UserAvatarAdminRestControllerTest extends AbstractTest {

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
    void deleteUserAvatarAdminTest() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .delete()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // user with no DELETE permission will get FORBIDDEN
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .delete()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .delete()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .delete()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void getUserAvatarAdminTest() throws IOException {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        byte[] bytes = Files.readAllBytes(avatar.toPath());
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .get()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .contentType("image/jpeg")
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        avatarByteArray = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .pathParam("id", "123")
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
        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .log().all()
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());

        resetExpectation();
        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getUserAvatarAdminBadRequestTest() throws IOException {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        byte[] bytes = Files.readAllBytes(avatar.toPath());
        // do not send content type and dont send image
        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        // do not send content type and send image
        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        // do not set body but send content type
        resetExpectation();
        mockServerClient.when(request().withPath("/internal/avatar/123")
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
                .pathParam("id", "123")
                .get()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void uploadAvatarAdminTest() {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        given()
                .when()
                .contentType("image/jpg")
                .body(avatar)
                .pathParam("id", "123")
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
                .pathParam("id", "123")
                .post()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType("image/jpg")
                .body(avatar)
                .pathParam("id", "123")
                .post()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();

        var error = given()
                .when()
                .queryParam("refType", RefTypeDTO.SMALL)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType("image/jpg")
                .pathParam("id", "123")
                .body(avatar)
                .post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void testAvatarAdminBadRequestWithoutProblemResponse() {
        var response = given()
                .when()
                .queryParam("refType", RefTypeDTO.MEDIUM)
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_NO_CONTENT)
                .contentType(APPLICATION_JSON)
                .pathParam("id", "123")
                .get();

        response.then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        assertThat(response.getBody().prettyPrint()).isEmpty();
    }

}
