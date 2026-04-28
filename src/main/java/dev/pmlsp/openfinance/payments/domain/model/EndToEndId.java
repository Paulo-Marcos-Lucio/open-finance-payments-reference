package dev.pmlsp.openfinance.payments.domain.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public record EndToEndId(String value) {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    public EndToEndId {
        Objects.requireNonNull(value, "value");
        if (value.length() != 32) {
            throw new IllegalArgumentException("endToEndId must be 32 chars (ISO 20022), got " + value.length());
        }
    }

    public static EndToEndId generate(Ispb debtorIspb, Instant now) {
        String ts = TS_FMT.format(now);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        String raw = "E" + debtorIspb.value() + ts + suffix;
        if (raw.length() < 32) {
            raw = raw + "0".repeat(32 - raw.length());
        } else if (raw.length() > 32) {
            raw = raw.substring(0, 32);
        }
        return new EndToEndId(raw);
    }
}
