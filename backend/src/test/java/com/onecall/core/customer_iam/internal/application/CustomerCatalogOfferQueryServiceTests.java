package com.onecall.core.customer_iam.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerCatalogOfferQueryServiceTests {
    private final UUID account = UUID.randomUUID();
    private final UUID organization = UUID.randomUUID();
    private final TrustedCustomerCaller personal = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    private final TrustedCustomerCaller enterprise = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, organization));
    private final DSLContext context = mock(DSLContext.class);
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final JooqCustomerBusinessNumberAccessRepository access = mock(JooqCustomerBusinessNumberAccessRepository.class);
    private final CustomerCatalogOfferReader reader = mock(CustomerCatalogOfferReader.class);
    private CustomerCatalogOfferQueryService service;

    @BeforeEach
    void setup() {
        when(transaction.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
        var role = mock(org.jooq.Record.class); when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of());
        when(reader.fetchBenefits(anySet())).thenReturn(Map.of());
        service = new CustomerCatalogOfferQueryService(transaction, context, access, reader);
    }

    private CustomerCatalogOfferQueryService.Request request(Integer limit) {
        return new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.ofNullable(limit), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private OfferRow offer(String code, int versionNo, UUID offerId, UUID versionId, String type, String audience, Long price) {
        String cycle = type.equals("QUOTE") ? "QUOTE" : type.equals("SUBSCRIPTION") ? "MONTHLY" : "ONCE";
        return new OfferRow(code, versionId, offerId, "WEB", versionNo, type, audience, price, "CNY", cycle, 1, "stored-" + offerId);
    }
    private OfferRow personalOffer(String code, UUID offerId, UUID versionId) { return offer(code, 1, offerId, versionId, "SUBSCRIPTION", "PERSONAL", 1200L); }
    private void failure(CustomerContextException.Code expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class,
                error -> { assertThat(error.code()).isEqualTo(expected); assertThat(error.getCause()).isNull(); });
    }

    @Test
    void defaultBoundsAndLookaheadRequestAreExact() {
        for (Integer limit : new Integer[]{null, 1, 20, 100}) {
            service.listCustomerCatalogOffers(personal, request(limit));
            verify(reader).fetchOffers(new Scope("WEB", "PERSONAL", Optional.empty()), Optional.empty(), (limit == null ? 20 : limit) + 1);
            clearInvocations(reader);
        }
        for (int limit : new int[]{-1, 0, 101, Integer.MAX_VALUE}) failure(CustomerContextException.Code.VALIDATION_FAILED, () -> request(limit));
    }

    @Test
    void trustedWorkspaceDerivesAudienceAndLifecycleIsRequiredBeforeReaders() {
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerCatalogOffers(new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB), request(1)));
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(access).requireActiveContext(any());
        failure(CustomerContextException.Code.FORBIDDEN, () -> service.listCustomerCatalogOffers(personal, request(1)));
        verifyNoInteractions(reader);
        var order = inOrder(context, access);
        order.verify(context).execute("SET LOCAL ROLE customer_iam_role");
        order.verify(context).fetchOne("select current_user");
        order.verify(access).requireActiveContext(new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope(account, null));
        reset(access, reader);
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of());
        when(reader.fetchBenefits(anySet())).thenReturn(Map.of());
        service.listCustomerCatalogOffers(enterprise, request(1));
        verify(access).requireActiveContext(new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope(account, organization));
        verify(reader).fetchOffers(new Scope("WEB", "ENTERPRISE", Optional.empty()), Optional.empty(), 2);
    }

    @Test
    void activeChannelAudienceAndOfferTypeNarrowingReachTheOwnerReader() {
        service.listCustomerCatalogOffers(personal, new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(10),
                Optional.of("WEB"), Optional.of("PERSONAL"), Optional.of("SUBSCRIPTION")));
        verify(reader).fetchOffers(new Scope("WEB", "PERSONAL", Optional.of("SUBSCRIPTION")), Optional.empty(), 11);
        clearInvocations(reader);
        service.listCustomerCatalogOffers(enterprise, new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(10),
                Optional.empty(), Optional.empty(), Optional.of("QUOTE")));
        verify(reader).fetchOffers(new Scope("WEB", "ENTERPRISE", Optional.of("QUOTE")), Optional.empty(), 11);
    }

    @Test
    void mismatchingRequestChannelOrAudienceIsRejectedBeforeAnyReaderCall() {
        for (var bad : List.of(
                new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(1), Optional.of("MINIAPP"), Optional.empty(), Optional.empty()),
                new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(1), Optional.empty(), Optional.of("ENTERPRISE"), Optional.empty())))
            failure(CustomerContextException.Code.FORBIDDEN, () -> service.listCustomerCatalogOffers(personal, bad));
        failure(CustomerContextException.Code.FORBIDDEN, () -> service.listCustomerCatalogOffers(enterprise,
                new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(1), Optional.empty(), Optional.of("PERSONAL"), Optional.empty())));
        verifyNoInteractions(reader, transaction);
    }

    @Test
    void quoteProjectsExplicitNullPriceAndOnlyStoredVersionToken() {
        UUID offerId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        var row = new OfferRow("BETA", versionId, offerId, "WEB", 1, "QUOTE", "ENTERPRISE", null, "CNY", "QUOTE", 1, "stored-token-xyz");
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(row));
        var item = service.listCustomerCatalogOffers(enterprise, request(1)).items().getFirst();
        assertThat(item.priceCents()).isNull();
        assertThat(item.currency()).isEqualTo("CNY");
        assertThat(item.offerType()).isEqualTo("QUOTE");
        assertThat(item.audience()).isEqualTo("ENTERPRISE");
        assertThat(item.versionToken()).isEqualTo("stored-token-xyz");
        assertThat(item.benefitLines()).isEmpty();
    }

    @Test
    void lookaheadUsesLastReturnedOfferAndNullCursorOnTail() {
        UUID firstOffer = UUID.randomUUID(); UUID secondOffer = UUID.randomUUID();
        var first = personalOffer("ALPHA", firstOffer, UUID.randomUUID());
        var second = personalOffer("BETA", secondOffer, UUID.randomUUID());
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(first, second));
        var page = service.listCustomerCatalogOffers(personal, request(1));
        assertThat(page.items()).hasSize(1);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextAnchor()).contains(new Anchor("ALPHA", 1, firstOffer));
        when(reader.fetchOffers(any(), eq(Optional.of(new Anchor("ALPHA", 1, firstOffer))), anyInt())).thenReturn(List.of(second));
        var tail = service.listCustomerCatalogOffers(personal, new CustomerCatalogOfferQueryService.Request(
                Optional.of(new Anchor("ALPHA", 1, firstOffer)), Optional.of(1), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(tail.items()).hasSize(1);
        assertThat(tail.hasMore()).isFalse();
        assertThat(tail.nextAnchor()).isEmpty();
    }

    @Test
    void emptySingleAndMultiPageProjectionIsExact() {
        var empty = service.listCustomerCatalogOffers(personal, request(20));
        assertThat(empty.items()).isEmpty(); assertThat(empty.nextAnchor()).isEmpty(); assertThat(empty.hasMore()).isFalse();
        UUID singleOffer = UUID.randomUUID();
        var single = personalOffer("ALPHA", singleOffer, UUID.randomUUID());
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(single));
        var one = service.listCustomerCatalogOffers(personal, request(20));
        assertThat(one.items()).hasSize(1); assertThat(one.hasMore()).isFalse(); assertThat(one.nextAnchor()).isEmpty();
        UUID offerA = UUID.randomUUID(); UUID offerB = UUID.randomUUID(); UUID offerC = UUID.randomUUID();
        var rowA = personalOffer("ALPHA", offerA, UUID.randomUUID());
        var rowB = personalOffer("BETA", offerB, UUID.randomUUID());
        var rowC = personalOffer("GAMMA", offerC, UUID.randomUUID());
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(rowA, rowB, rowC));
        var firstPage = service.listCustomerCatalogOffers(personal, request(2));
        assertThat(firstPage.items()).hasSize(2); assertThat(firstPage.hasMore()).isTrue();
        assertThat(firstPage.nextAnchor()).contains(new Anchor("BETA", 1, offerB));
        when(reader.fetchOffers(any(), eq(Optional.of(new Anchor("BETA", 1, offerB))), anyInt())).thenReturn(List.of(rowC));
        var lastPage = service.listCustomerCatalogOffers(personal, new CustomerCatalogOfferQueryService.Request(
                Optional.of(new Anchor("BETA", 1, offerB)), Optional.of(2), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(lastPage.items()).hasSize(1); assertThat(lastPage.hasMore()).isFalse(); assertThat(lastPage.nextAnchor()).isEmpty();
    }

    @Test
    void emptyBenefitLinesAreValidAndCanonicalOrderIsEntitlementTypeAscending() {
        UUID offerId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        var row = personalOffer("ALPHA", offerId, versionId);
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(row));
        assertThat(service.listCustomerCatalogOffers(personal, request(1)).items().getFirst().benefitLines()).isEmpty();
        when(reader.fetchBenefits(anySet())).thenReturn(Map.of(versionId, List.of(
                new BenefitRow(versionId, "AI_SECONDS", 10, 30, 1),
                new BenefitRow(versionId, "EVIDENCE_COUNT", 3, 30, 2),
                new BenefitRow(versionId, "RECORDING_SECONDS", 60, 30, 1),
                new BenefitRow(versionId, "STORAGE_BYTES", 1024, 30, 0))));
        var lines = service.listCustomerCatalogOffers(personal, request(1)).items().getFirst().benefitLines();
        assertThat(lines).extracting(CustomerCatalogOfferQueryService.BenefitLine::entitlementType)
                .containsExactly("AI_SECONDS", "EVIDENCE_COUNT", "RECORDING_SECONDS", "STORAGE_BYTES");
        assertThat(lines.getFirst().quantity()).isEqualTo(10);
        assertThat(lines.getFirst().grantIntervalMonths()).isEqualTo(1);
    }

    @Test
    void nonMonotonicDuplicateOrCorruptOfferFactsFailClosed() {
        UUID offerA = UUID.randomUUID(); UUID offerB = UUID.randomUUID();
        var alpha = personalOffer("ALPHA", offerA, UUID.randomUUID());
        var beta = personalOffer("BETA", offerB, UUID.randomUUID());
        var duplicate = personalOffer("BETA", offerA, UUID.randomUUID());
        for (var rows : List.of(List.of(alpha, duplicate), List.of(beta, alpha))) {
            when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(rows);
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerCatalogOffers(personal, request(5)));
        }
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(alpha));
        failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerCatalogOffers(personal,
                new CustomerCatalogOfferQueryService.Request(Optional.of(new Anchor("ALPHA", 1, offerA)), Optional.of(5), Optional.empty(), Optional.empty(), Optional.empty())));
        UUID versionId = UUID.randomUUID();
        for (var corrupt : List.of(
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "USD", "MONTHLY", 1, "t"),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "BOGUS", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "t"),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "SUBSCRIPTION", "PERSONAL", null, "CNY", "MONTHLY", 1, "t"),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "QUOTE", "PERSONAL", null, "CNY", "QUOTE", 1, "t"),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 0, "t"),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "x".repeat(129)),
                new OfferRow("ALPHA", versionId, offerA, "WEB", 0, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "t"),
                new OfferRow("ALPHA", versionId, offerA, "MINIAPP", 1, "SUBSCRIPTION", "PERSONAL", 1200L, "CNY", "MONTHLY", 1, "t"))) {
            when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(corrupt));
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerCatalogOffers(personal, request(5)));
        }
    }

    @Test
    void duplicateOrphanOrOutOfRangeBenefitFactsFailClosed() {
        UUID offerId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        when(reader.fetchOffers(any(), any(), anyInt())).thenReturn(List.of(personalOffer("ALPHA", offerId, versionId)));
        for (var benefits : List.of(
                Map.of(versionId, List.of(new BenefitRow(versionId, "AI_SECONDS", 1, 30, 0), new BenefitRow(versionId, "AI_SECONDS", 2, 30, 0))),
                Map.of(UUID.randomUUID(), List.of(new BenefitRow(UUID.randomUUID(), "AI_SECONDS", 1, 30, 0))),
                Map.of(versionId, List.of(new BenefitRow(versionId, "AI_SECONDS", 0, 30, 0))),
                Map.of(versionId, List.of(new BenefitRow(versionId, "AI_SECONDS", 1, 0, 0))),
                Map.of(versionId, List.of(new BenefitRow(versionId, "AI_SECONDS", 1, 30, -1))),
                Map.of(versionId, List.of(new BenefitRow(versionId, "BOGUS", 1, 30, 0))),
                Map.of(versionId, List.of(new BenefitRow(versionId, "STORAGE_BYTES", 1, 30, 0), new BenefitRow(versionId, "AI_SECONDS", 1, 30, 0))))) {
            when(reader.fetchBenefits(anySet())).thenReturn(benefits);
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerCatalogOffers(personal, request(1)));
        }
    }

    @Test
    void technicalAndProjectionFailuresReturnNoPartialOrSensitivePage() {
        for (RuntimeException exception : List.of(new org.jooq.exception.DataAccessException("PASSWORD_SQL_CANARY"), new IllegalStateException("CIPHERTEXT_CANARY"))) {
            doThrow(exception).when(reader).fetchOffers(any(), any(), anyInt());
            failure(exception instanceof org.jooq.exception.DataAccessException ? CustomerContextException.Code.SERVICE_UNAVAILABLE : CustomerContextException.Code.INTERNAL,
                    () -> service.listCustomerCatalogOffers(personal, request(1)));
        }
        doReturn(null).when(reader).fetchOffers(any(), any(), anyInt());
        failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerCatalogOffers(personal, request(1)));
        when(context.fetchOne("select current_user")).thenReturn(null);
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerCatalogOffers(personal, request(1)));
        when(context.execute("SET LOCAL ROLE customer_iam_role")).thenThrow(new IllegalStateException("SQL_CANARY"));
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerCatalogOffers(personal, request(1)));
    }
}
