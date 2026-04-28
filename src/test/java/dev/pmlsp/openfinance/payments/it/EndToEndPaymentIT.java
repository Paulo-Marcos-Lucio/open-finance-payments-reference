package dev.pmlsp.openfinance.payments.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = {
        "ofpayments.payment.settle-delay-ms=50"
})
class EndToEndPaymentIT extends AbstractIntegrationIT {

    @Test
    void consentLifecycleAndPaymentSettlement() throws Exception {
        Map<String, Object> consentReq = new HashMap<>();
        consentReq.put("loggedUser", Map.of(
                "name", "Fulano",
                "document", "12345678901",
                "documentType", "CPF"));
        consentReq.put("creditor", Map.of(
                "ispb", "60746948",
                "issuer", "0001",
                "number", "00012345-6",
                "type", "CACC"));
        consentReq.put("amount", "75.50");
        consentReq.put("currency", "BRL");

        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> consentResponse = http().exchange(
                baseUrl() + "/open-banking/payments/v1/consents",
                HttpMethod.POST,
                new HttpEntity<>(consentReq, json),
                Map.class);
        assertTrue(consentResponse.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = consentResponse.getBody();
        assertNotNull(body);
        String consentId = (String) body.get("consentId");
        assertEquals("RCVD", body.get("status"));

        ResponseEntity<Map> auth = http().exchange(
                baseUrl() + "/sim/holder/consents/" + consentId + "/authorise",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), json),
                Map.class);
        assertTrue(auth.getStatusCode().is2xxSuccessful());
        assertEquals(true, auth.getBody().get("authorised"));

        Map<String, Object> paymentReq = Map.of(
                "consentId", consentId,
                "debtor", Map.of(
                        "ispb", "99988877",
                        "issuer", "0002",
                        "number", "1234567",
                        "type", "CACC"));

        ResponseEntity<Map> paymentResponse = http().exchange(
                baseUrl() + "/open-banking/payments/v1/pix/payments",
                HttpMethod.POST,
                new HttpEntity<>(paymentReq, json),
                Map.class);
        assertTrue(paymentResponse.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> pbody = paymentResponse.getBody();
        assertNotNull(pbody);
        String paymentId = (String) pbody.get("paymentId");
        assertEquals("ACSP", pbody.get("status"));
        assertEquals(32, ((String) pbody.get("endToEndId")).length());

        String terminalStatus = pollUntilTerminal(paymentId, 5000);
        assertEquals("ACSC", terminalStatus);

        ResponseEntity<Map> finalConsent = http().getForEntity(
                baseUrl() + "/open-banking/payments/v1/consents/" + consentId,
                Map.class);
        assertEquals("CONSUMED", finalConsent.getBody().get("status"));
    }

    private String pollUntilTerminal(String paymentId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            ResponseEntity<Map> r = http().getForEntity(
                    baseUrl() + "/open-banking/payments/v1/pix/payments/" + paymentId,
                    Map.class);
            last = (String) r.getBody().get("status");
            if ("ACSC".equals(last) || "RJCT".equals(last)) {
                return last;
            }
            Thread.sleep(100);
        }
        return last;
    }
}
