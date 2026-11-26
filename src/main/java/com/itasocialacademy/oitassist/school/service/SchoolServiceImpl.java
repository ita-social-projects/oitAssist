package com.itasocialacademy.oitassist.school.service;

import com.itasocialacademy.oitassist.school.dao.dto.response.SchoolResponse;
import com.itasocialacademy.oitassist.school.dao.repository.SchoolRepository;
import com.itasocialacademy.oitassist.school.exceptions.SchoolNotFoundException;
import com.itasocialacademy.oitassist.school.mapper.SchoolMapper;
import com.itasocialacademy.oitassist.school.service.interfaces.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {
    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolResponse> searchSchools(String districtCode, String search, Pageable pageable) {
        String normalizedSearch = (search == null) ? "" : search.trim();
        Pageable stablePageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().and(Sort.by(Sort.Direction.ASC, "id")));
        return schoolRepository.searchByDistrictAndText(districtCode, normalizedSearch, stablePageable)
            .map(schoolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse getSchoolById(Long id) {
        return schoolRepository.findById(id)
            .map(schoolMapper::toResponse)
            .orElseThrow(() -> new SchoolNotFoundException(id));
    }
}
