@ApplicationModule(
    displayName = "Participation Requests",
    allowedDependencies = {
        "security::SecurityFacade",
        "core",
        "competition::api",
        "competition::dto",
        "competition::exceptions",
        "competition::enums",
        "competition::spi",
        "user::UserFacade",
        "user::UserAuthDetails",
        "user::Role",
        "user::UserProfileDetails",
        "user::UserNotFoundException"
    })
package com.itasocialacademy.oitassist.participation;

import org.springframework.modulith.ApplicationModule;