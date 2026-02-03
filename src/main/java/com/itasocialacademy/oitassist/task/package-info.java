@ApplicationModule(
    displayName = "Task",
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
    })
package com.itasocialacademy.oitassist.task;

import org.springframework.modulith.ApplicationModule;