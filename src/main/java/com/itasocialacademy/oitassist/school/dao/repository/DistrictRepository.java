package com.itasocialacademy.oitassist.school.dao.repository;

import com.itasocialacademy.oitassist.school.dao.model.District;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, String> {
    List<District> findAllByRegionCodeOrderByNameAsc(String regionCode);
}
