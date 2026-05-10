@ApplicationModule(
    allowedDependencies = {
        "core",
        "core::GlobalExceptionHandler",
        "core::AuthenticationException",
        "user::UserFacade"})
package com.itasocialacademy.oitassist.security;

import org.springframework.modulith.ApplicationModule;