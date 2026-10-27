package com.onecall.archfixtures.internal.consumer;

import com.onecall.archfixtures.internal.provider.internal.Internal;

public class Consumer {

    public String use() {
        return new Internal().value();
    }
}
