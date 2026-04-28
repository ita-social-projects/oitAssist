@ApplicationModule(
    displayName = "File Manager",
    allowedDependencies = {
        "core",
        "core::RestController",
        "core::AbstractRestControllerImpl",
        "core::BaseService",
        "core::AbstractServiceImpl",
        "core::GeneralMapper",
        "core::DtoMapper",
        "core::CreateMapper",
        "core::EntityDTO",
        "core::CreateEntityDTO",
        "core::UpdateEntityDTO",
        "core::LongEntity",
        "core::EntityRepository",
        "core::DtoMapper",
        "core::CreateMapper",
        "security::SecurityFacade"
    })
package com.itasocialacademy.oitassist.filemanager;

import org.springframework.modulith.ApplicationModule;