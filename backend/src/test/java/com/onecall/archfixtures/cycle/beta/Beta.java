package com.onecall.archfixtures.cycle.beta;

import com.onecall.archfixtures.cycle.alpha.Alpha;

public class Beta {

    public Alpha alpha() {
        return new Alpha();
    }
}
