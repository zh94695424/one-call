package com.onecall.core.customer_iam.internal.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;

public final class CustomerShareCommandDtos {
    private CustomerShareCommandDtos() { }

    public enum VerificationMode { NONE, EXTRACTION_CODE, DESIGNATED_MOBILE, ORGANIZATION_MEMBER }
    public enum ShareStatus { ACTIVE, EXPIRED, REVOKED, LOCKED }

    public record CreateRequest(String creationRequestId, UUID evidenceId, VerificationMode verificationMode,
            Instant expiresAt, boolean downloadAllowed, Integer maxAdmissions, String designatedMobile) { }

    public record ShareState(String shareId, UUID evidenceId, VerificationMode verificationMode, ShareStatus status,
            @JsonSerialize(using = UtcInstantSerializer.class) Instant validFrom,
            @JsonSerialize(using = UtcInstantSerializer.class) Instant expiresAt,
            Integer maxAdmissions, int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration) { }

    public record ShareCreated(String shareId, UUID evidenceId, VerificationMode verificationMode, ShareStatus status,
            @JsonSerialize(using = UtcInstantSerializer.class) Instant validFrom,
            @JsonSerialize(using = UtcInstantSerializer.class) Instant expiresAt,
            Integer maxAdmissions, int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration,
            String capabilityToken, @JsonInclude(JsonInclude.Include.NON_NULL) String extractionCode) { }

    public static final class UtcInstantSerializer extends StdSerializer<Instant> {
        public UtcInstantSerializer() { super(Instant.class); }

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(new DateTimeFormatterBuilder().appendInstant(value.getNano() == 0 ? 0 : 6)
                    .toFormatter().format(value));
        }
    }
}
