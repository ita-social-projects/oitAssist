package com.itasocialacademy.oitassist.school.service.interfaces;

import com.itasocialacademy.oitassist.school.dao.dto.response.DistrictResponse;
import com.itasocialacademy.oitassist.school.dao.dto.response.RegionResponse;
import java.util.List;

public interface DirectoryService {
    List<RegionResponse> getAllRegions();

    List<DistrictResponse> getDistrictsByRegion(String regionCode);
}
