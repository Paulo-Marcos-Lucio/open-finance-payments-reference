package dev.pmlsp.openfinance.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenFinancePaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenFinancePaymentsApplication.class, args);
    }
}
