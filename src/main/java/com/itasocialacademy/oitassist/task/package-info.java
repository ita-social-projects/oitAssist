@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "filemanager :: FilesAttachRequestedEvent", "filemanager :: RelatedEntityType",
        "filemanager :: FilesDetachRequestedEvent", "security :: SecurityFacade", "user :: UserNotFoundException",
        "user :: UserAuthDetails", "user :: Role", "user :: UserFacade"})
package com.itasocialacademy.oitassist.task;

import org.springframework.modulith.ApplicationModule;