package com.itasocialacademy.oitassist.filemanager.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends EntityRepository<FileAsset, Long>, JpaSpecificationExecutor<FileAsset> {
}