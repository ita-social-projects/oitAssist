@ApplicationModule(
    displayName = "Task Assignment",
    allowedDependencies = {"core", "competition :: exceptions", "competition :: api", "competition :: dto",
        "task :: exceptions", "task :: dto", "task :: api", "task :: events", "filemanager :: api",
        "filemanager :: FileRole", "filemanager :: RelatedEntityType", "security :: SecurityFacade",
        "filemanager :: dto"})
package com.itasocialacademy.oitassist.taskassignment;

import org.springframework.modulith.ApplicationModule;