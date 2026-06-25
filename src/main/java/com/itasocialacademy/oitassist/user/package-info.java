@ApplicationModule(
    displayName = "User",
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
        "security::UserDetailsImpl",
        "security::SecurityUserProvider",
        "security::SecurityFacade",
        "security::OAuthUserProvisioningPort"
    })
package com.itasocialacademy.oitassist.user;

import org.springframework.modulith.ApplicationModule;