package com.onecall.archfixtures.cycle.alpha;

import com.onecall.archfixtures.cycle.beta.Beta;

public class Alpha {

    public Beta beta() {
        return new Beta();
    }
}
