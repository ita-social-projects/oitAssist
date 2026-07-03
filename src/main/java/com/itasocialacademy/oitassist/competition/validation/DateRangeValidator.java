package com.itasocialacademy.oitassist.competition.validation;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates the {@link ValidDateRange} constraint by checking that a
 * {@link HasDateRange} DTO's {@code dateFinish} is strictly after its
 * {@code dateStart}.
 *
 * <p>
 * On failure, the default class-level violation is suppressed and a violation
 * is instead attached to the {@code dateFinish} property, so that clients
 * receive a field-scoped error rather than a generic object-level one.
 * </p>
 *
 * @see ValidDateRange
 * @see HasDateRange
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, HasDateRange> {

    @Override
    public boolean isValid(HasDateRange value, ConstraintValidatorContext context) {
        if (value.dateStart() == null || value.dateFinish() == null) {
            return true;
        }

        boolean valid = value.dateFinish().isAfter(value.dateStart());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dateFinish")
                .addConstraintViolation();
        }

        return valid;
    }
}
