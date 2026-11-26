package com.itasocialacademy.oitassist.school.api.facade;

import com.itasocialacademy.oitassist.school.api.dto.SchoolDetail;
import com.itasocialacademy.oitassist.school.api.interfaces.SchoolFacade;
import com.itasocialacademy.oitassist.school.dao.repository.SchoolRepository;
import com.itasocialacademy.oitassist.school.mapper.SchoolMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link SchoolFacade}.
 */
@Service
@RequiredArgsConstructor
public class SchoolFacadeImpl implements SchoolFacade {
    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SchoolDetail> findSchoolById(Long schoolId) {
        if (schoolId == null) {
            return Optional.empty();
        }

        return schoolRepository.findDetailedById(schoolId)
            .map(schoolMapper::toDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSchoolById(Long schoolId) {
        if (schoolId == null) {
            return false;
        }
        return schoolRepository.existsById(schoolId);
    }
}
