@ApplicationModule(
    displayName = "Participation Requests",
    allowedDependencies = {"security::SecurityFacade", "core", "competition::api", "competition::dto",
        "competition::exceptions", "competition::enums", "competition :: spi"})
package com.itasocialacademy.oitassist.participation;

import org.springframework.modulith.ApplicationModule;