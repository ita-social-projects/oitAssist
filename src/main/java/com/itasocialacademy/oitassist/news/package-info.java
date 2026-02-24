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
    "user :: UserDetailsImpl",
})
package com.itasocialacademy.oitassist.news;

import org.springframework.modulith.ApplicationModule;