package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.policy.NewsFilePolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NewsFileValidationStrategyTest {

    private final NewsFileValidationStrategy strategy = new NewsFileValidationStrategy();

    @Test
    void supports_ShouldReturnNews_WhenCalled() {
        assertEquals(RelatedEntityType.NEWS, strategy.supports());
    }

    @Test
    void resolvePolicy_ShouldReturnNewsFilePolicyInstance_WhenCalled() {
        assertSame(NewsFilePolicy.INSTANCE, strategy.resolvePolicy(1L));
    }
}
