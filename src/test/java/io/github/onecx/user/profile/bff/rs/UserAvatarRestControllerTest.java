package io.github.onecx.user.profile.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
import io.github.onecx.user.profile.bff.rs.controllers.UserAvatarRestController;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@LogService
@TestHTTPEndpoint(UserAvatarRestController.class)
class UserAvatarRestControllerTest extends AbstractTest {

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
    void deleteUserAvatarTest() {
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .delete()
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
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
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
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
    void getUserAvatarTest() throws IOException {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        byte[] bytes = Files.readAllBytes(avatar.toPath());
        given()
                .when()
                .contentType(APPLICATION_JSON)
                .get("/avatarId1")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar/avatarId1")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_OCTET_STREAM)
                        .withBody(bytes));

        var avatarByteArray = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .get("/avatarId1")
                .then()
                .contentType("application/octet-stream")
                .statusCode(OK.getStatusCode())
                .extract().asByteArray();

        assertThat(avatarByteArray).isNotNull().isEqualTo(bytes);

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar/avatarId1")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .get("/avatarId1")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .log().all()
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());

        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar/avatarId2")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
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
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        ImageInfo info = new ImageInfo();
        info.setId("avatarId1");
        info.setUserUploaded(Boolean.FALSE);
        info.setImageUrl("/image/url/big/avatar");
        info.setSmallImageUrl("/image/url/small/avatar");
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
                .withMethod(HttpMethod.GET))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(info)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo(info.getImageUrl());
        assertThat(response.getSmallImageUrl()).isEqualTo(info.getSmallImageUrl());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
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
    void uploadAvatarTest() {
        File avatar = new File("src/test/resources/data/avatar_test.jpg");
        given()
                .when()
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        ImageInfo info = new ImageInfo();
        info.setId("avatarId1");
        info.setUserUploaded(Boolean.FALSE);
        info.setImageUrl("/image/url/big/avatar");
        info.setSmallImageUrl("/image/url/small/avatar");
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
                .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(info)));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(ImageInfoDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo(info.getImageUrl());
        assertThat(response.getSmallImageUrl()).isEqualTo(info.getSmallImageUrl());

        ProblemDetailResponse problem = new ProblemDetailResponse();
        problem.setErrorCode("MANUAL_ERROR");
        problem.setDetail("Manual detail of error");
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
                .withMethod(HttpMethod.PUT))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(problem)));

        var error = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType("image/jpg")
                .body(avatar)
                .put()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo(problem.getErrorCode());
        assertThat(error.getDetail()).isEqualTo(problem.getDetail());
    }

    @Test
    void testBadRequestWithoutProblemResponse() {
        mockServerClient.when(request().withPath("/v1/userProfile/me/avatar")
                .withMethod(HttpMethod.GET))
                .withPriority(200)
                .respond(httpRequest -> response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        var response = given()
                .when()
                .header(APM_HEADER_PARAM, createToken("user1", null))
                .contentType(APPLICATION_JSON)
                .get();

        response.then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        assertThat(response.getBody().prettyPrint()).isEmpty();
    }

}
