package com.itasocialacademy.oitassist.school.dao.repository;

import com.itasocialacademy.oitassist.school.dao.model.School;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolRepository extends JpaRepository<School, Long> {
    @Query("SELECT s FROM School s WHERE s.district.code = :districtCode "
        + "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) "
        + "OR LOWER(s.settlement) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<School> searchByDistrictAndText(
        @Param("districtCode") String districtCode,
        @Param("search") String search,
        Pageable pageable);

    @EntityGraph(attributePaths = {"district", "district.region"})
    Optional<School> findDetailedById(Long id);
}
