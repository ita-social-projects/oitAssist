@ApplicationModule(
    displayName = "Competition History",
    allowedDependencies = {
        "core",
        "security::SecurityFacade",
        "competition::api",
        "competition::dto",
        "competition::enums",
        "participation::api",
        "evaluation::api",
        "evaluation::dto"
    })
package com.itasocialacademy.oitassist.competitionhistory;

import org.springframework.modulith.ApplicationModule;