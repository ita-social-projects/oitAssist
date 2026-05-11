@ApplicationModule(
    allowedDependencies = {
        "core",
        "core::GlobalExceptionHandler",
        "core::AuthenticationException",
        "user::UserFacade",
        "user :: UserAuthDetails",
        "user :: Role"})
package com.itasocialacademy.oitassist.security;

import org.springframework.modulith.ApplicationModule;