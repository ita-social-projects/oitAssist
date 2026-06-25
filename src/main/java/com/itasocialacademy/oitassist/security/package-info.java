@ApplicationModule(
    allowedDependencies = {
        "core",
        "core::GlobalExceptionHandler",
        "core::AuthenticationException"
    })
package com.itasocialacademy.oitassist.security;

import org.springframework.modulith.ApplicationModule;
