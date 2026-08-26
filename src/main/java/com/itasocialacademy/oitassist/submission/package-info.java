@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "security :: SecurityFacade", "filemanager :: api",
        "filemanager :: RelatedEntityType", "filemanager :: FileRole", "filemanager :: dto",
        "taskassignment :: api", "taskassignment :: dto", "taskassignment :: exceptions",
        "filemanager :: FilesDetachAllRequestedEvent"})
package com.itasocialacademy.oitassist.submission;

import org.springframework.modulith.ApplicationModule;