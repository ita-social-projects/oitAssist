@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "filemanager :: RelatedEntityType",
        "security :: SecurityFacade", "user :: UserNotFoundException",
        "user :: UserAuthDetails", "user :: Role", "user :: UserFacade", "filemanager :: dto", "filemanager :: api",
        "filemanager :: FileRole"})
package com.itasocialacademy.oitassist.task;

import org.springframework.modulith.ApplicationModule;