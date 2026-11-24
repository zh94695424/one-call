package com.onecall.core.customer_iam.internal.web;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.web.CustomerCatalogOfferCursorCodec.Anchor;
import com.onecall.core.customer_iam.internal.web.CustomerCatalogOfferCursorCodec.Binding;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class CustomerCatalogOfferCursorCodecTests {
    private final UUID account = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID organization = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID offerId = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private final Binding personal = new Binding(account, null, "WEB", "PERSONAL", Optional.empty());
    private final Binding enterprise = new Binding(account, organization, "WEB", "ENTERPRISE", Optional.empty());
    private final Anchor anchor = new Anchor("ALPHA", 1, offerId);
    private final CustomerCatalogOfferCursorCodec codec = codec(new byte[32]);

    private static CustomerCatalogOfferCursorCodec codec(byte[] key) {
        var properties = new CustomerCatalogOfferCursorProperties();
        properties.setCursorHmacKey(Base64.getEncoder().encodeToString(key));
        return new CustomerCatalogOfferCursorCodec(properties);
    }

    private void invalid(String value) {
        assertThatThrownBy(() -> codec.decode(personal, value)).isInstanceOfSatisfying(CustomerContextException.class,
                failure -> { assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID); assertThat(failure.getCause()).isNull(); });
    }

    private String signed(byte[] bytes) throws Exception {
        int macStart = 86 + (bytes[65] & 0xff);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
        mac.update("onecall:customer-catalog-offers:cursor:v1\n".getBytes(StandardCharsets.UTF_8));
        byte[] signature = mac.doFinal(Arrays.copyOf(bytes, macStart));
        System.arraycopy(signature, 0, bytes, macStart, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    void personalAndOrganizationRoundTripsAreCanonicalAndDeterministic() {
        for (var binding : List.of(personal, enterprise)) {
            String value = codec.encode(binding, anchor);
            assertThat(codec.decode(binding, value)).isEqualTo(anchor);
            assertThat(codec.encode(binding, anchor)).isEqualTo(value);
            assertThat(value).matches("[A-Za-z0-9_-]+");
        }
        assertThat(codec.encode(personal, anchor)).isNotEqualTo(codec.encode(enterprise, anchor));
    }

    @Test
    void payloadVersionLengthDomainAndAnchorAreExact() throws Exception {
        var shortAnchor = new Anchor("AB", 1, offerId);
        var longAnchor = new Anchor("A" + "B".repeat(63), 7, offerId);
        String shortValue = codec.encode(personal, shortAnchor);
        String longValue = codec.encode(personal, longAnchor);
        assertThat(Base64.getUrlDecoder().decode(shortValue)).hasSize(120);
        assertThat(shortValue).hasSize(160);
        assertThat(Base64.getUrlDecoder().decode(longValue)).hasSize(182);
        assertThat(longValue).hasSize(243);
        assertThat(codec.decode(personal, longValue)).isEqualTo(longAnchor);
        byte[] bytes = Base64.getUrlDecoder().decode(shortValue);
        assertThat(bytes[0]).isEqualTo((byte) 1);
        assertThat(Arrays.copyOfRange(bytes, 1, 33)).isEqualTo(MessageDigest.getInstance("SHA-256").digest(
                ("onecall:customer-catalog-offers:v1\nproductCode:asc,versionNo:desc,catalogOfferId:asc\n" + account + "\nPERSONAL\n").getBytes(StandardCharsets.UTF_8)));
        assertThat(Arrays.copyOfRange(bytes, 33, 65)).isEqualTo(MessageDigest.getInstance("SHA-256").digest(
                "onecall:customer-catalog-offers:filters:v1\nWEB\nPERSONAL\nALL\n".getBytes(StandardCharsets.UTF_8)));
        assertThat(bytes[65]).isEqualTo((byte) 2);
        assertThat(ByteBuffer.wrap(bytes).getInt(68)).isEqualTo(1);
        assertThat(signed(bytes)).isEqualTo(shortValue);
        String padded = Base64.getUrlEncoder().encodeToString(Base64.getUrlDecoder().decode(longValue));
        assertThat(padded).hasSize(244);
        invalid(padded);
    }

    @Test
    void invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected() {
        String value = codec.encode(personal, anchor);
        for (String candidate : List.of("", value + "=", " " + value, value + "\n", "+" + value.substring(1), "/" + value.substring(1), "a".repeat(10000))) invalid(candidate);
    }

    @Test
    void truncatedExtendedUnknownVersionAndInvalidAnchorAreRejected() throws Exception {
        String value = codec.encode(personal, anchor);
        invalid(value.substring(1)); invalid(value + "a");
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        bytes[0] = 2; invalid(signed(bytes)); bytes[0] = 1;
        bytes[65] = 1; invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        bytes[65] = 65; invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        bytes[65] = 6; invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        bytes[65] = 5;
        ByteBuffer.wrap(bytes).putInt(71, 0); invalid(signed(bytes));
        ByteBuffer.wrap(bytes).putInt(71, 1);
        bytes[66] = 'a'; invalid(signed(bytes)); bytes[66] = 'A';
        assertThat(signed(bytes)).isEqualTo(value);
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
        for (var binding : List.of(new Binding(UUID.randomUUID(), null, "WEB", "PERSONAL", Optional.empty()),
                enterprise, new Binding(UUID.randomUUID(), organization, "WEB", "ENTERPRISE", Optional.empty())))
            assertThatThrownBy(() -> codec.decode(binding, value)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID));
        assertThatThrownBy(() -> codec.decode(new Binding(account, UUID.randomUUID(), "WEB", "ENTERPRISE", Optional.empty()), codec.encode(enterprise, anchor)))
                .isInstanceOf(CustomerContextException.class);
    }

    @Test
    void normalizedFilterChangeIsFilterMismatchNotInvalid() {
        String value = codec.encode(personal, anchor);
        for (var binding : List.of(new Binding(account, null, "WEB", "PERSONAL", Optional.of("SUBSCRIPTION")),
                new Binding(account, null, "MINIAPP", "PERSONAL", Optional.empty()),
                new Binding(account, null, "WEB", "ENTERPRISE", Optional.empty())))
            assertThatThrownBy(() -> codec.decode(binding, value)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_FILTER_MISMATCH));
    }

    @Test
    void missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure() {
        for (String value : List.of("", " ", "SECRET_CANARY", Base64.getEncoder().encodeToString(new byte[31]), Base64.getEncoder().encodeToString(new byte[33]), Base64.getEncoder().withoutPadding().encodeToString(new byte[32]))) {
            var properties = new CustomerCatalogOfferCursorProperties(); properties.setCursorHmacKey(value);
            assertThatThrownBy(() -> new CustomerCatalogOfferCursorCodec(properties)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> { assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE); assertThat(failure.getMessage()).isEqualTo("SERVICE_UNAVAILABLE"); assertThat(failure.getCause()).isNull(); });
        }
    }

    @Test
    void keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope() {
        byte[] changed = new byte[32]; changed[0] = 1;
        String value = codec.encode(personal, anchor);
        assertThatThrownBy(() -> codec(changed).decode(personal, value)).isInstanceOfSatisfying(CustomerContextException.class,
                failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.CURSOR_INVALID));
        assertThat(codec.decode(personal, value)).isEqualTo(anchor);
        var caller = new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(account,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB,
                new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(
                        com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null));
        var transaction = org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class);
        org.mockito.Mockito.when(transaction.execute(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
                ((org.springframework.transaction.support.TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class)));
        var context = org.mockito.Mockito.mock(org.jooq.DSLContext.class);
        var role = org.mockito.Mockito.mock(org.jooq.Record.class);
        org.mockito.Mockito.when(role.get(0, String.class)).thenReturn("customer_iam_role");
        org.mockito.Mockito.when(context.fetchOne("select current_user")).thenReturn(role);
        var reader = org.mockito.Mockito.mock(com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.class);
        UUID secondOffer = UUID.randomUUID();
        var first = new com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.OfferRow("ALPHA", UUID.randomUUID(), offerId, "WEB", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "t");
        var second = new com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.OfferRow("BETA", UUID.randomUUID(), secondOffer, "WEB", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "t");
        var readerScope = new com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.Scope("WEB", "PERSONAL", Optional.empty());
        org.mockito.Mockito.when(reader.fetchOffers(readerScope, Optional.empty(), 2)).thenReturn(List.of(first, second));
        org.mockito.Mockito.when(reader.fetchBenefits(org.mockito.ArgumentMatchers.anySet())).thenReturn(java.util.Map.of());
        var service = new com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService(transaction, context,
                org.mockito.Mockito.mock(com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository.class), reader);
        var firstPage = service.listCustomerCatalogOffers(caller, new com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService.Request(
                Optional.empty(), Optional.of(1), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(firstPage.items()).hasSize(1); assertThat(firstPage.hasMore()).isTrue();
        var next = firstPage.nextAnchor().orElseThrow();
        String continuation = codec.encode(personal, new Anchor(next.productCode(), next.versionNo(), next.catalogOfferId()));
        assertThat(continuation).isEqualTo(value);
        org.mockito.Mockito.when(reader.fetchOffers(readerScope, Optional.of(new com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.Anchor("ALPHA", 1, offerId)), 101)).thenReturn(List.of(second));
        var continuationPage = service.listCustomerCatalogOffers(caller, new com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService.Request(
                Optional.of(new com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.Anchor("ALPHA", 1, offerId)), Optional.of(100), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(continuationPage.items()).hasSize(1);
        assertThat(continuationPage.items().getFirst().catalogOfferId()).isEqualTo(secondOffer);
        assertThat(continuationPage.hasMore()).isFalse(); assertThat(continuationPage.nextAnchor()).isEmpty();
        assertThat(codec.decode(personal, continuation)).isEqualTo(anchor);
    }
}
