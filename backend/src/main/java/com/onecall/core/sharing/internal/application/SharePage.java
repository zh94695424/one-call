package com.onecall.core.sharing.internal.application;

import java.util.List;
import java.util.Optional;

public record SharePage(List<ShareSummary> items, Optional<ShareCursor> nextCursor, boolean hasMore) {
    public SharePage {
        items = List.copyOf(items);
        if (nextCursor == null || hasMore != nextCursor.isPresent() || (hasMore && items.isEmpty())) {
            throw new IllegalArgumentException("Invalid share page");
        }
    }
}
