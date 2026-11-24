package com.onecall.core.customer_iam.internal.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.customer.catalog-offers")
public final class CustomerCatalogOfferCursorProperties {
    private String cursorHmacKey = "";

    public String getCursorHmacKey() { return cursorHmacKey; }
    public void setCursorHmacKey(String value) { cursorHmacKey = value; }
}
