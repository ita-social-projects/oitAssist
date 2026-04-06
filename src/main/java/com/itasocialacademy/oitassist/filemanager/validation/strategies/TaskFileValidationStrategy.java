package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.policy.TaskFilePolicy;
import org.springframework.stereotype.Component;

@Component
public class TaskFileValidationStrategy extends AbstractFileValidationStrategy {
    @Override
    public RelatedEntityType supports() {
        return RelatedEntityType.TASK;
    }

    @Override
    protected FilePolicy resolvePolicy(Long relatedEntityId) {
        return TaskFilePolicy.INSTANCE;
    }
}
