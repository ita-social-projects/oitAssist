@ApplicationModule(
    displayName = "File Manager",
    allowedDependencies = {
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
        "core::DtoMapper",
        "core::CreateMapper",
    })
package com.itasocialacademy.oitassist.filemanager;

import org.springframework.modulith.ApplicationModule;