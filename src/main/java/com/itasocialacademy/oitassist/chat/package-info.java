@ApplicationModule(
    displayName = "Chat",
    allowedDependencies = {
        "core",
        "security :: SecurityFacade",
        "task :: TaskForumFacade",
        "task :: TaskForumContext"
    })
package com.itasocialacademy.oitassist.chat;

import org.springframework.modulith.ApplicationModule;