@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "filemanager :: FilesAttachRequestedEvent", "filemanager :: RelatedEntityType"})
package com.itasocialacademy.oitassist.task;

import org.springframework.modulith.ApplicationModule;