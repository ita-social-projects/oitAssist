@ApplicationModule(
    displayName = "Competition",
    allowedDependencies = {
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
    "task::TaskFacade",
    "task::ResponseTaskDTO",
})
package com.itasocialacademy.oitassist.competition;

import org.springframework.modulith.ApplicationModule;