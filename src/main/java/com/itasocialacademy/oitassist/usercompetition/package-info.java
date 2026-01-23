@ApplicationModule(allowedDependencies = {
    "user::UserEntity",
    "competition::CompetitionEntity"
})
package com.itasocialacademy.oitassist.usercompetition;

import org.springframework.modulith.ApplicationModule;