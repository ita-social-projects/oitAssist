@ApplicationModule(
    allowedDependencies = {
        "core::GlobalExceptionHandler",
        "core::AuthenticationException",
        "user::UserFacade",
        "user::UserDetailsImpl"
    })
package com.itasocialacademy.oitassist.security;

import org.springframework.modulith.ApplicationModule;