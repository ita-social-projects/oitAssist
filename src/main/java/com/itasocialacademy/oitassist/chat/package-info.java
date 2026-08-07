@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {"core", "core :: AuthenticationException", "security :: SecurityFacade",
        "task :: api", "task :: dto", "task :: exceptions"})
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;