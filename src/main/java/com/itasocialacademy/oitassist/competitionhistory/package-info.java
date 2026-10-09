@ApplicationModule(
    displayName = "Competition History",
    allowedDependencies = {
        "core",
        "security::SecurityFacade",
        "competition::api",
        "competition::dto",
        "competition::enums",
        "participation::api"
    })
package com.itasocialacademy.oitassist.competitionhistory;

import org.springframework.modulith.ApplicationModule;