package com.itasocialacademy.oitassist.logfile.dao;

import java.util.List;

public interface LogFileDao {
    List<LogFileMetadata> findAll();
}
