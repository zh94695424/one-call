package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.sharing.internal.application.SharePage;
import com.onecall.core.sharing.internal.application.ShareSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerSharePage(List<Item> items, String nextCursor, boolean hasMore) {
    public CustomerSharePage { items = List.copyOf(items); }

    public static CustomerSharePage from(SharePage page, String nextCursor) {
        return new CustomerSharePage(page.items().stream().map(Item::from).toList(), nextCursor, page.hasMore());
    }

    public record Item(String shareId, UUID evidenceId, ShareSummary.VerificationMode verificationMode,
            ShareSummary.ShareStatus status, Instant validFrom, Instant expiresAt, Integer maxAdmissions,
            int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration) {
        static Item from(ShareSummary summary) {
            return new Item(summary.shareId(), summary.evidenceId(), summary.verificationMode(), summary.status(),
                    summary.validFrom(), summary.expiresAt(), summary.maxAdmissions(), summary.admissionsUsed(),
                    summary.downloadAllowed(), summary.codeGeneration(), summary.revocationGeneration());
        }
    }
}
