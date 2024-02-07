package org.tkit.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

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
                .get("/avatarId1")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        mockServerClient.when(request().withPath("/internal/userProfile/me/avatar/avatarId1")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_OCTET_STREAM)
                        .withBody(bytes));

        var avatarByteArray = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get("/avatarId1")
                .then()
                .contentType("application/octet-stream")
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        // standard USER with READ permission is enough
        avatarByteArray = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .get("/avatarId1")
                .then()
                .contentType("application/octet-stream")
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/internal/userProfile/me/avatar/avatarId1")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get("/avatarId1")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .log().all()
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());

        mockServerClient.when(request().withPath("/internal/userProfile/me/avatar/avatarId2")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/avatarId2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void getUserAvatarInfoTest() {
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
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo("/image/url/big/avatar");
        assertThat(response.getSmallImageUrl()).isEqualTo("/image/url/small/avatar");

        // standard USER with READ permission can also make GET
        response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo("/image/url/big/avatar");
        assertThat(response.getSmallImageUrl()).isEqualTo("/image/url/small/avatar");

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

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void uploadAvatarTest() {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        given()
                .when()
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // USER with no WRITE permission will get FORBIDDEN
        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo("/image/url/big/avatar");
        assertThat(response.getSmallImageUrl()).isEqualTo("/image/url/small/avatar");

        var error = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .header(CUSTOM_FLOW_HEADER, CFH_ERROR_WITH_CONTENT)
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("MANUAL_ERROR");
        assertThat(error.getDetail()).isEqualTo("Manual detail of error");
    }

    @Test
    void testBadRequestWithoutProblemResponse() {
        var response = given()
                .when()
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
