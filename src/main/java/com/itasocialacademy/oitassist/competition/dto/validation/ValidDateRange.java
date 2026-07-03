package com.itasocialacademy.oitassist.competition.dto.validation;

import com.itasocialacademy.oitassist.competition.validation.DateRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint asserting that a {@link HasDateRange} DTO's {@code dateFinish} occurs strictly after its
 * {@code dateStart}.
 *
 * <p>
 * This is intentionally a self-consistency check only: it validates the range carried by a single DTO in isolation and
 * does not compare against any parent entity's dates.
 * </p>
 *
 * <p>
 * If either {@code dateStart} or {@code dateFinish} is {@code null}, this constraint reports no violation and defers to
 * the individual field's own {@code @NotNull} annotation.
 * </p>
 *
 * <p>
 * Usage:
 * </p>
 * <pre>{@code
 * @ValidDateRange
 * public record CreateStageRequest(
 *     @NotBlank String title,
 *     @NotNull ZonedDateTime dateStart,
 *     @NotNull ZonedDateTime dateFinish
 * ) implements HasDateRange {}
 * }</pre>
 *
 * @see HasDateRange
 * @see DateRangeValidator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    /**
     * @return the error message used when {@code dateFinish} is not after {@code dateStart}
     */
    String message() default "dateFinish must be after dateStart";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}