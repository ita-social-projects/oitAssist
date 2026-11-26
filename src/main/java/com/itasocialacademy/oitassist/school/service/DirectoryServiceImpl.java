package com.itasocialacademy.oitassist.school.service;

import com.itasocialacademy.oitassist.school.dao.dto.response.DistrictResponse;
import com.itasocialacademy.oitassist.school.dao.dto.response.RegionResponse;
import com.itasocialacademy.oitassist.school.dao.repository.DistrictRepository;
import com.itasocialacademy.oitassist.school.dao.repository.RegionRepository;
import com.itasocialacademy.oitassist.school.mapper.DistrictMapper;
import com.itasocialacademy.oitassist.school.mapper.RegionMapper;
import com.itasocialacademy.oitassist.school.service.interfaces.DirectoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final RegionMapper regionMapper;
    private final DistrictMapper districtMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RegionResponse> getAllRegions() {
        return regionRepository.findAllByOrderByNameAsc()
            .stream()
            .map(regionMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictResponse> getDistrictsByRegion(String regionCode) {
        return districtRepository.findAllByRegionCodeOrderByNameAsc(regionCode)
            .stream()
            .map(districtMapper::toResponse)
            .toList();
    }
}
