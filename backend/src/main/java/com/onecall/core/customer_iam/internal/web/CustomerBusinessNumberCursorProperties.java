package com.onecall.core.customer_iam.internal.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.customer.business-numbers")
public final class CustomerBusinessNumberCursorProperties {
    private String cursorHmacKey = "";

    public String getCursorHmacKey() { return cursorHmacKey; }
    public void setCursorHmacKey(String value) { cursorHmacKey = value; }
}
