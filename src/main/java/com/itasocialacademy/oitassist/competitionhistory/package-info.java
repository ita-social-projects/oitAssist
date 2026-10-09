@ApplicationModule(
    displayName = "Competition History",
    allowedDependencies = {
        "core",
        "security::SecurityFacade",
        "competition::api",
        "competition::dto",
        "competition::enums",
        "participation::api",
        "user::Role"
    })
package com.itasocialacademy.oitassist.competitionhistory;

import org.springframework.modulith.ApplicationModule;