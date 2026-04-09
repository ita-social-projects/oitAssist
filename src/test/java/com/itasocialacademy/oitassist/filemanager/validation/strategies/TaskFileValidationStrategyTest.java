package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.policy.TaskFilePolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskFileValidationStrategyTest {

    private final TaskFileValidationStrategy strategy = new TaskFileValidationStrategy();

    @Test
    void supports_ShouldReturnTask_WhenCalled() {
        assertEquals(RelatedEntityType.TASK, strategy.supports());
    }

    @Test
    void resolvePolicy_ShouldReturnTaskFilePolicyInstance_WhenCalled() {
        assertSame(TaskFilePolicy.INSTANCE, strategy.resolvePolicy(1L));
    }
}