@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {"core", "security :: SecurityFacade",
        "task :: TaskBodyFacade"})
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;