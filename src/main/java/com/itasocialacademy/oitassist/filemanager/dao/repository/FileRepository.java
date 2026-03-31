package com.itasocialacademy.oitassist.filemanager.dao.repository;

import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileAsset, Long>, JpaSpecificationExecutor<FileAsset> {
}