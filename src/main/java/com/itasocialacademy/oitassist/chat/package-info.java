@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {"core", "security :: SecurityFacade",
        "task :: api"})
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;