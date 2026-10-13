package com.itasocialacademy.oitassist.logfile.dao;

import com.itasocialacademy.oitassist.logfile.dao.model.LogFileMetadata;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface LogFileDao {
    List<LogFileMetadata> findAll();

    List<LogFileMetadata> findByNameContainingIgnoreCase(String name);

    Optional<Path> downloadFile(String fileName);
}
