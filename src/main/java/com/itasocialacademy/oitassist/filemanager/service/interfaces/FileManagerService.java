package com.itasocialacademy.oitassist.filemanager.service.interfaces;

import java.io.FileNotFoundException;

public interface FileManagerService {
    /**
     * Method to upload a file and create a data record in the database.
     * @return id of the new file record in the DB.
     */
    Long upload();

    /**
     * Method to mark a file SOFT_DELETED, but keep a physical file intact.
     * @param fileId id of the file record in the db.
     * @throws FileNotFoundException Thrown if files is not found in the database.
     */
    void deleteSoft(Long fileId) throws FileNotFoundException;

    /**
     * Method to mark a file HARD_DELETED, and call StorageProvider to physically delete the file.
     * @param fileId id of the file record in the db.
     * @throws FileNotFoundException Thrown if files is not found in the database.
     */
    void deleteHard(Long fileId) throws FileNotFoundException;
}
