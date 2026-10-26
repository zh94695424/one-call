package com.onecall.archtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.onecall.core.OneCallApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = OneCallApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OneCallApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithoutDatabaseOrSecurityAutoConfiguration() {
        assertThat(context.getBean(OneCallApplication.class)).isNotNull();
        assertThat(context.containsBean("dataSource")).isFalse();
        assertThat(context.containsBean("flyway")).isFalse();
        assertThat(context.containsBean("springSecurityFilterChain")).isFalse();
    }
}
