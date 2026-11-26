package com.itasocialacademy.oitassist.school.api.interfaces;

import com.itasocialacademy.oitassist.school.api.dto.SchoolDetail;
import java.util.Optional;
import org.springframework.modulith.NamedInterface;

/**
 * Cross-module entry point into the {@code school} module.
 *
 * <p>
 * This facade is the only sanctioned way for other Spring application modules
 * (e.g. {@code user}, {@code competition}) to invoke {@code school}-owned use
 * cases.
 * </p>
 */
@NamedInterface("SchoolFacade")
public interface SchoolFacade {
    /**
     * Finds school details by its unique identifier.
     *
     * @param schoolId the ID of the school
     * @return an {@code Optional} containing {@link SchoolDetail} if found, or
     *         empty otherwise
     */
    Optional<SchoolDetail> findSchoolById(Long schoolId);

    /**
     * Checks if a school with the given ID exists.
     *
     * @param schoolId the ID of the school
     * @return {@code true} if the school exists, {@code false} otherwise
     */
    boolean existsSchoolById(Long schoolId);
}
