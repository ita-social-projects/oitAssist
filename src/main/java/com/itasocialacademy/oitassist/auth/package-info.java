@ApplicationModule(
    displayName = "Auth",
    allowedDependencies = {
        "core",
        "user::UserEntity",
        "user::UserRepository",
        "user::UserAlreadyExistsException",
        "user::UserNotFoundException"
    })
package com.itasocialacademy.oitassist.auth;

import org.springframework.modulith.ApplicationModule;