package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.policy.NewsFilePolicy;
import org.springframework.stereotype.Component;

@Component
public class NewsFileValidationStrategy extends AbstractFileValidationStrategy {
    /**
     * {@inheritDoc}
     */
    @Override
    public RelatedEntityType supports() {
        return RelatedEntityType.NEWS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FilePolicy resolvePolicy(Long relatedEntityId) {
        return NewsFilePolicy.INSTANCE;
    }
}