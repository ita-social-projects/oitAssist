@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {"core", "core :: AuthenticationException",
        "security :: SecurityFacade", "security :: StompAuthenticationFacade",
        "taskassignment :: api", "taskassignment :: dto",
        "taskassignment :: enums", "taskassignment :: exceptions",
        "competition :: api", "competition :: dto",
        "competition :: enums", "competition :: exceptions",
        "participation :: api",
        "user :: UserFacade", "user :: ForumResponderCandidate",
        "user :: Role", "user :: UserStatus"})
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;