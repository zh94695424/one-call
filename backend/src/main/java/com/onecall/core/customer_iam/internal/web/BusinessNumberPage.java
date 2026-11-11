package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService;
import java.util.List;
import java.util.UUID;

public record BusinessNumberPage(List<Item> items, String nextCursor, boolean hasMore) {
    public BusinessNumberPage { items = List.copyOf(items); }
    public enum NumberType { MOBILE, LANDLINE }
    public enum BindingStatus { ACTIVE, DISABLED, RETIRED }
    public enum ActivationStatus { INACTIVE, ACTIVE, SUSPENDED }
    public enum RealNameStatus { UNVERIFIED, VERIFIED }
    public record Item(UUID bindingId, UUID subjectId, String numberMasked, NumberType numberType,
            BindingStatus bindingStatus, ActivationStatus activationStatus, RealNameStatus realNameStatus) { }

    static BusinessNumberPage from(CustomerBusinessNumberQueryService.Page page, String cursor) {
        return new BusinessNumberPage(page.items().stream().map(item -> new Item(item.bindingId(), item.subjectId(),
                item.numberMasked(), NumberType.valueOf(item.numberType()), BindingStatus.valueOf(item.bindingStatus()),
                ActivationStatus.valueOf(item.activationStatus()), RealNameStatus.valueOf(item.realNameStatus()))).toList(), cursor, page.hasMore());
    }
}
