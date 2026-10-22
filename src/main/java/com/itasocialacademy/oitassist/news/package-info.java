@ApplicationModule(allowedDependencies = {
    "core",
    "security::UserDetailsImpl",
    "security::SecurityFacade",
    "filemanager::FilesAttachRequestedEvent",
    "filemanager::FilesDetachRequestedEvent",
    "filemanager::RelatedEntityType",
    "filemanager::FileAccessValidator"
})
package com.itasocialacademy.oitassist.news;

import org.springframework.modulith.ApplicationModule;