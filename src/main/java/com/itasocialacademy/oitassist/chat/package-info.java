@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {"core", "core :: AuthenticationException", "security :: SecurityFacade",
        "taskassignment :: api", "taskassignment :: dto",
        "taskassignment :: enums", "taskassignment :: exceptions",
        "competition :: api", "competition :: dto",
        "competition :: enums", "competition :: exceptions",
        "participation :: api"})
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;