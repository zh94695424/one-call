package com.onecall.archfixtures.forbidden.consumer;

import com.onecall.archfixtures.forbidden.provider.Provider;

public class Consumer {

    public String use() {
        return new Provider().name();
    }
}
