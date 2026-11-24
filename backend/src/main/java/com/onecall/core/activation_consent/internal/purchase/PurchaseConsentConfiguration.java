package com.onecall.core.activation_consent.internal.purchase;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PurchaseConsentConfiguration.CurrentPolicy.class)
@org.springframework.transaction.annotation.EnableTransactionManagement
@Lazy
public class PurchaseConsentConfiguration {
    @ConfigurationProperties("onecall.purchase-consent.current")
    public record CurrentPolicy(String policyCode, String policyVersion, String promptVersion, String contentSha256) {
        boolean valid() { return PurchaseConsentSemantics.validText(policyCode) && PurchaseConsentSemantics.validText(policyVersion) && PurchaseConsentSemantics.validText(promptVersion)
                && contentSha256 != null && contentSha256.matches("[0-9a-f]{64}"); }
    }
    @Bean
    PurchaseConsentService purchaseConsentService(@Qualifier("operationsSubjectReadDataSource") DataSource source,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadCatalogSaleTransactionManager") DataSourceTransactionManager manager, CurrentPolicy policy) {
        if (manager.getDataSource() != source) throw new IllegalStateException("Purchase consent unavailable");
        return new DefaultPurchaseConsentService(new JooqPurchaseConsentRepository(source, dsl), policy);
    }
}
