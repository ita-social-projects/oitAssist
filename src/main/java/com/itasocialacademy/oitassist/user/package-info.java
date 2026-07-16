@ApplicationModule(
    displayName = "User",
    allowedDependencies = {
        "core",
        "security::UserDetailsImpl",
        "security::SecurityUserProvider",
        "security::SecurityFacade",
        "security::OAuthUserProvisioningPort"
    })
package com.itasocialacademy.oitassist.user;

import org.springframework.modulith.ApplicationModule;