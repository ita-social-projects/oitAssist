package com.itasocialacademy.oitassist.school.service.interfaces;

import com.itasocialacademy.oitassist.school.dao.dto.response.SchoolResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchoolService {
    Page<SchoolResponse> searchSchools(String districtCode, String search, Pageable pageable);

    SchoolResponse getSchoolById(Long id);
}
