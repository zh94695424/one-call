package com.onecall.core.customer_iam.internal.web;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Anchor;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class CustomerBusinessNumberCursorCodecTests {
    private final Scope personal = new Scope(UUID.fromString("10000000-0000-0000-0000-000000000001"), null);
    private final Scope organization = new Scope(personal.accountId(), UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private final Anchor anchor = new Anchor(Instant.parse("2026-11-11T00:00:00.123456Z"), UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private final CustomerBusinessNumberCursorCodec codec = codec(new byte[32]);

    private static CustomerBusinessNumberCursorCodec codec(byte[] key) {
        var properties = new CustomerBusinessNumberCursorProperties();
        properties.setCursorHmacKey(Base64.getEncoder().encodeToString(key));
        return new CustomerBusinessNumberCursorCodec(properties);
    }

    private void invalid(String value) {
        assertThatThrownBy(() -> codec.decode(personal, value)).isInstanceOfSatisfying(CustomerContextException.class,
                failure -> { assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID); assertThat(failure.getCause()).isNull(); });
    }

    private String signed(byte[] bytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
        mac.update("onecall:customer-business-numbers:cursor:v1\n".getBytes(StandardCharsets.UTF_8));
        byte[] signature = mac.doFinal(Arrays.copyOf(bytes, 61));
        System.arraycopy(signature, 0, bytes, 61, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    void personalAndOrganizationRoundTripsAreCanonicalAndDeterministic() {
        for (var scope : java.util.List.of(personal, organization)) {
            String value = codec.encode(scope, anchor);
            assertThat(codec.decode(scope, value)).isEqualTo(anchor);
            assertThat(codec.encode(scope, anchor)).isEqualTo(value);
            assertThat(value).hasSize(124).matches("[A-Za-z0-9_-]+");
        }
        assertThat(codec.encode(personal, anchor)).isNotEqualTo(codec.encode(organization, anchor));
    }

    @Test
    void payloadVersionLengthDomainAndMicrosecondAnchorAreExact() throws Exception {
        byte[] bytes = Base64.getUrlDecoder().decode(codec.encode(personal, anchor));
        assertThat(bytes).hasSize(93);
        assertThat(bytes[0]).isEqualTo((byte) 1);
        assertThat(Arrays.copyOfRange(bytes, 1, 33)).isEqualTo(MessageDigest.getInstance("SHA-256").digest(
                ("onecall:customer-business-numbers:v1\nsubject.createdAt:desc,bindingId:asc\n" + personal.accountId() + "\nPERSONAL\n").getBytes(StandardCharsets.UTF_8)));
        assertThat(ByteBuffer.wrap(bytes, 33, 12).getLong()).isEqualTo(anchor.createdAt().getEpochSecond());
        assertThat(ByteBuffer.wrap(bytes, 41, 4).getInt()).isEqualTo(123456000);
        assertThat(signed(bytes)).isEqualTo(codec.encode(personal, anchor));
        for (String time : java.util.List.of("-4712-01-01T00:00:00Z", "+294276-12-31T23:59:59.999999Z")) {
            var boundary = new Anchor(Instant.parse(time), anchor.bindingId());
            assertThat(codec.decode(personal, codec.encode(personal, boundary))).isEqualTo(boundary);
        }
    }

    @Test
    void invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected() {
        String value = codec.encode(personal, anchor);
        for (String candidate : java.util.List.of("", value + "=", " " + value, value + "\n", "+" + value.substring(1), "/" + value.substring(1), "a".repeat(10000))) invalid(candidate);
    }

    @Test
    void truncatedExtendedUnknownVersionAndInvalidTimestampAreRejected() throws Exception {
        String value = codec.encode(personal, anchor);
        invalid(value.substring(1)); invalid(value + "a");
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        bytes[0] = 2; invalid(signed(bytes)); bytes[0] = 1;
        for (int nanos : new int[]{-1, 1, 999999999, 1000000000}) {
            ByteBuffer.wrap(bytes).putInt(41, nanos); invalid(signed(bytes));
        }
        ByteBuffer.wrap(bytes).putInt(41, 0);
        for (long seconds : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, Instant.parse("-4713-01-01T00:00:00Z").getEpochSecond(), Instant.parse("+294277-01-01T00:00:00Z").getEpochSecond()}) {
            ByteBuffer.wrap(bytes).putLong(33, seconds); invalid(signed(bytes));
        }
    }

    @Test
    void everyPayloadOrMacByteMutationIsRejected() {
        byte[] original = Base64.getUrlDecoder().decode(codec.encode(personal, anchor));
        for (int index = 0; index < original.length; index++) {
            byte[] changed = original.clone(); changed[index] ^= 1;
            invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(changed));
        }
    }

    @Test
    void accountWorkspaceKindAndOrganizationReplayAreRejected() {
        String value = codec.encode(personal, anchor);
        for (var scope : java.util.List.of(new Scope(UUID.randomUUID(), null), organization, new Scope(UUID.randomUUID(), organization.organizationId()))) {
            assertThatThrownBy(() -> codec.decode(scope, value)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID));
        }
        assertThatThrownBy(() -> codec.decode(new Scope(personal.accountId(), UUID.randomUUID()), codec.encode(organization, anchor)))
                .isInstanceOf(CustomerContextException.class);
    }

    @Test
    void missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure() {
        for (String value : java.util.List.of("", " ", "SECRET_CANARY", Base64.getEncoder().encodeToString(new byte[31]), Base64.getEncoder().encodeToString(new byte[33]), Base64.getEncoder().withoutPadding().encodeToString(new byte[32]))) {
            var properties = new CustomerBusinessNumberCursorProperties(); properties.setCursorHmacKey(value);
            assertThatThrownBy(() -> new CustomerBusinessNumberCursorCodec(properties)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> { assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE); assertThat(failure.getMessage()).isEqualTo("SERVICE_UNAVAILABLE"); assertThat(failure.getCause()).isNull(); });
        }
    }

    @Test
    void keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope() {
        byte[] changed = new byte[32]; changed[0] = 1;
        String value = codec.encode(personal, anchor);
        assertThatThrownBy(() -> codec(changed).decode(personal, value)).isInstanceOfSatisfying(CustomerContextException.class,
                failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID));
        assertThat(codec.decode(new Scope(personal.accountId(), null), value)).isEqualTo(anchor);
        var caller = new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(personal.accountId(),
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB,
                new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(
                        com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null));
        var scope = com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService.requireScope(caller);
        var transaction = org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class);
        org.mockito.Mockito.when(transaction.execute(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
                ((org.springframework.transaction.support.TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class)));
        var context = org.mockito.Mockito.mock(org.jooq.DSLContext.class);
        var role = org.mockito.Mockito.mock(org.jooq.Record.class);
        org.mockito.Mockito.when(role.get(0, String.class)).thenReturn("customer_iam_role");
        org.mockito.Mockito.when(context.fetchOne("select current_user")).thenReturn(role);
        var reader = org.mockito.Mockito.mock(com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.class);
        var first = new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Row(anchor.bindingId(), UUID.randomUUID(), "138****0000", "MOBILE", "ACTIVE", "VERIFIED", anchor.createdAt());
        var second = new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Row(UUID.randomUUID(), UUID.randomUUID(), "138****0001", "MOBILE", "ACTIVE", "VERIFIED", anchor.createdAt().minusSeconds(1));
        org.mockito.Mockito.when(reader.fetch(scope, java.util.Optional.empty(), 2)).thenReturn(java.util.List.of(first, second));
        var service = new com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService(transaction, context,
                org.mockito.Mockito.mock(com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository.class), reader, subjects -> java.util.Map.of());
        var firstPage = service.listCustomerBusinessNumbers(caller,
                new com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService.Request(java.util.Optional.empty(), java.util.Optional.of(1)));
        assertThat(firstPage.items()).hasSize(1); assertThat(firstPage.hasMore()).isTrue();
        String continuation = codec.encode(scope, firstPage.nextAnchor().orElseThrow());
        assertThat(continuation).isEqualTo(value);
        org.mockito.Mockito.when(reader.fetch(scope, java.util.Optional.of(anchor), 101)).thenReturn(java.util.List.of(second));
        var continuationPage = service.listCustomerBusinessNumbers(caller,
                new com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService.Request(
                        java.util.Optional.of(codec.decode(scope, continuation)), java.util.Optional.of(100)));
        assertThat(continuationPage.items()).hasSize(1);
        assertThat(continuationPage.items().getFirst().bindingId()).isEqualTo(second.bindingId());
        assertThat(continuationPage.hasMore()).isFalse(); assertThat(continuationPage.nextAnchor()).isEmpty();
        org.mockito.Mockito.verify(reader).fetch(scope, java.util.Optional.empty(), 2);
        org.mockito.Mockito.verify(reader).fetch(scope, java.util.Optional.of(anchor), 101);
        org.mockito.Mockito.verifyNoMoreInteractions(reader);
    }
}
