package com.itasocialacademy.oitassist.school.dao.repository;

import com.itasocialacademy.oitassist.school.dao.model.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, String> {
    List<Region> findAllByOrderByNameAsc();
}
