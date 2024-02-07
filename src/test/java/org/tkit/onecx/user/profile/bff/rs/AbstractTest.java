package org.tkit.onecx.user.profile.bff.rs;

import java.security.PrivateKey;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.Claims;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkiverse.mockserver.test.MockServerTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@QuarkusTestResource(MockServerTestResource.class)
public abstract class AbstractTest {

    protected static final String ADMIN = "alice";

    protected static final String USER = "bob";

    protected static final String MANUAL_ERROR = "MANUAL_ERROR";
    protected static final String MANUAL_ERROR_DETAIL = "Manual MANUAL_ERROR error detail";
    protected static final String OPTIMISTIC_LOCK = "OPTIMISTIC_LOCK";

    protected static final String CUSTOM_FLOW_HEADER = "custom-flow";

    protected static final String CFH_ERROR_WITH_CONTENT = "bad_error_content";

    protected static final String CFH_ERROR_NO_CONTENT = "bad_error_no_content";

    protected static final String CFH_ERROR_CONSTRAINT_VIOLATIONS = "bad_error_constraint_violations";

    protected static final String CFH_ERROR_OPT_LOCK = "bad_error_opt_lock";

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    protected static final String APM_HEADER_PARAM = ConfigProvider.getConfig()
            .getValue("%test.tkit.rs.context.token.header-param", String.class);

    protected static final String CLAIMS_ORG_ID = ConfigProvider.getConfig()
            .getValue("%test.tkit.rs.context.tenant-id.mock.claim-org-id", String.class);;

    static {
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(
                        (cls, charset) -> {
                            ObjectMapper objectMapper = new ObjectMapper();
                            objectMapper.registerModule(new JavaTimeModule());
                            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                            return objectMapper;
                        }));
    }

    protected static String createToken(String userId, String orgId) {
        try {
            String userName = userId != null ? userId : "test-user";
            String organizationId = orgId != null ? orgId : "org1";
            JsonObjectBuilder claims = Json.createObjectBuilder();
            claims.add(Claims.preferred_username.name(), userName);
            claims.add(Claims.sub.name(), userName);
            claims.add(CLAIMS_ORG_ID, organizationId);
            PrivateKey privateKey = KeyUtils.generateKeyPair(2048).getPrivate();
            return Jwt.claims(claims.build()).sign(privateKey);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
