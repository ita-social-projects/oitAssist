@ApplicationModule(
    displayName = "Auth",
    allowedDependencies = {
        "core",
        "user :: RegisterCommand",
        "user :: UserFacade",
        "user :: UserRegisteredEvent"})
package com.itasocialacademy.oitassist.auth;

import org.springframework.modulith.ApplicationModule;