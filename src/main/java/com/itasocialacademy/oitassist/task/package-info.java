@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "filemanager :: FilesAttachRequestedEvent", "filemanager :: RelatedEntityType",
        "security :: SecurityFacade"})
package com.itasocialacademy.oitassist.task;

import org.springframework.modulith.ApplicationModule;