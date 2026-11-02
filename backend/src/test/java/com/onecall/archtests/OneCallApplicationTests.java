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
        assertThat(context).isNotInstanceOf(org.springframework.web.context.WebApplicationContext.class);
        for (String name : java.util.List.of("customerBoundaryFilterRegistration", "customerMeController",
                "customerApiErrorController", "customerContextService", "customerContextRepository", "customerQueryDataSource")) {
            assertThat(context.containsBean(name)).as(name).isFalse();
        }
        for (Class<?> type : java.util.List.of(javax.sql.DataSource.class, org.jooq.DSLContext.class,
                org.springframework.transaction.PlatformTransactionManager.class,
                org.springframework.transaction.support.TransactionTemplate.class,
                com.zaxxer.hikari.HikariDataSource.class,
                com.onecall.core.sharing.internal.application.CustomerShareQueryService.class,
                com.onecall.core.sharing.internal.persistence.ShareQueryRepository.class)) {
            assertThat(context.getBeansOfType(type)).isEmpty();
        }
    }
}
