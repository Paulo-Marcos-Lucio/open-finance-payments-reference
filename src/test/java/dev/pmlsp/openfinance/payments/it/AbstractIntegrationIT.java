package dev.pmlsp.openfinance.payments.it;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=18082")
@ActiveProfiles({"local", "simulator"})
public abstract class AbstractIntegrationIT {

    @Value("${server.port}")
    protected int port;

    @DynamicPropertySource
    static void overrideHolder(DynamicPropertyRegistry registry) {
        registry.add("ofpayments.holder.base-url", () -> "http://localhost:18082/sim");
    }

    protected RestTemplate http() {
        return new RestTemplateBuilder().build();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
