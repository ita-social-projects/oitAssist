@ApplicationModule(allowedDependencies = {
    "core",
    "core::RestController",
    "core::AbstractRestControllerImpl",
    "core::BaseService",
    "core::AbstractServiceImpl",
    "core::GeneralMapper",
    "core::EntityDTO",
    "core::CreateEntityDTO",
    "core::UpdateEntityDTO",
    "core::LongEntity",
    "core::EntityRepository",
    "security::UserDetailsImpl",
    "security::SecurityFacade",
    "filemanager::FilesAttachRequestedEvent",
    "filemanager::RelatedEntityType"
})
package com.itasocialacademy.oitassist.news;

import org.springframework.modulith.ApplicationModule;