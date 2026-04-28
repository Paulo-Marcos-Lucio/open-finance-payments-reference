package dev.pmlsp.openfinance.payments.it;

import dev.pmlsp.openfinance.payments.security.DPoPHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=18083")
@ActiveProfiles({"local", "simulator", "fapi"})
@TestPropertySource(properties = {
        "ofpayments.payment.settle-delay-ms=50"
})
class FapiE2EIT {

    @Value("${server.port}")
    int port;

    @DynamicPropertySource
    static void overrideHolder(DynamicPropertyRegistry registry) {
        registry.add("ofpayments.holder.base-url", () -> "http://localhost:18083/sim");
    }

    private RestTemplate http() {
        return new RestTemplateBuilder().build();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void clientCanCreateConsentWithDpopBoundToken() {
        DPoPHelper dpop = new DPoPHelper();

        // 1) PISP client requests access token from mock auth, presenting DPoP proof
        String tokenUrl = baseUrl() + "/mock-auth/token";
        String tokenProof = dpop.sign("POST", tokenUrl);

        HttpHeaders tokenReqHeaders = new HttpHeaders();
        tokenReqHeaders.set("DPoP", tokenProof);
        tokenReqHeaders.set("X-Client-Id", "demo-pisp");

        ResponseEntity<Map> tokenResponse = http().exchange(tokenUrl, HttpMethod.POST,
                new HttpEntity<>(null, tokenReqHeaders), Map.class);
        assertTrue(tokenResponse.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenBody = tokenResponse.getBody();
        assertNotNull(tokenBody);
        String accessToken = (String) tokenBody.get("access_token");
        assertNotNull(accessToken);
        assertEquals("DPoP", tokenBody.get("token_type"));

        // 2) PISP client uses the bound token to create a consent
        String consentUrl = baseUrl() + "/open-banking/payments/v1/consents";
        String consentProof = dpop.sign("POST", consentUrl);

        Map<String, Object> body = new HashMap<>();
        body.put("loggedUser", Map.of(
                "name", "Fulano",
                "document", "12345678901",
                "documentType", "CPF"));
        body.put("creditor", Map.of(
                "ispb", "60746948",
                "issuer", "0001",
                "number", "00012345-6",
                "type", "CACC"));
        body.put("amount", "75.50");
        body.put("currency", "BRL");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "DPoP " + accessToken);
        headers.set("DPoP", consentProof);

        ResponseEntity<Map> consentResponse = http().exchange(consentUrl, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
        assertTrue(consentResponse.getStatusCode().is2xxSuccessful(),
                () -> "expected 201, got " + consentResponse.getStatusCode());
        assertEquals("RCVD", consentResponse.getBody().get("status"));
    }

    @Test
    void requestWithoutDpopHeadersIsRejected() {
        Map<String, Object> body = Map.of(
                "loggedUser", Map.of("name", "X", "document", "12345678901", "documentType", "CPF"),
                "creditor", Map.of("ispb", "60746948", "issuer", "0001", "number", "1", "type", "CACC"),
                "amount", "1.00",
                "currency", "BRL");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            http().exchange(baseUrl() + "/open-banking/payments/v1/consents",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            assertNotNull(e.getResponseHeaders().getFirst("WWW-Authenticate"));
            return;
        }
        throw new AssertionError("expected 401");
    }

    @Test
    void replayedTokenFromAnotherKeyIsRejected() {
        // First client gets a legit token
        DPoPHelper attacker = new DPoPHelper();
        DPoPHelper victim = new DPoPHelper();

        String tokenUrl = baseUrl() + "/mock-auth/token";
        HttpHeaders th = new HttpHeaders();
        th.set("DPoP", victim.sign("POST", tokenUrl));
        ResponseEntity<Map> r = http().exchange(tokenUrl, HttpMethod.POST,
                new HttpEntity<>(null, th), Map.class);
        @SuppressWarnings("unchecked")
        String stolenToken = (String) r.getBody().get("access_token");
        assertNotNull(stolenToken);

        // Attacker tries to use the stolen token with their own DPoP proof
        String consentUrl = baseUrl() + "/open-banking/payments/v1/consents";
        HttpHeaders attackHeaders = new HttpHeaders();
        attackHeaders.setContentType(MediaType.APPLICATION_JSON);
        attackHeaders.set("Authorization", "DPoP " + stolenToken);
        attackHeaders.set("DPoP", attacker.sign("POST", consentUrl));

        Map<String, Object> body = Map.of(
                "loggedUser", Map.of("name", "X", "document", "12345678901", "documentType", "CPF"),
                "creditor", Map.of("ispb", "60746948", "issuer", "0001", "number", "1", "type", "CACC"),
                "amount", "1.00",
                "currency", "BRL");

        try {
            http().exchange(consentUrl, HttpMethod.POST,
                    new HttpEntity<>(body, attackHeaders), Map.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            String www = e.getResponseHeaders().getFirst("WWW-Authenticate");
            assertNotNull(www);
            assertTrue(www.contains("invalid_token"), () -> "got: " + www);
            return;
        }
        throw new AssertionError("expected 401 due to thumbprint mismatch");
    }
}
