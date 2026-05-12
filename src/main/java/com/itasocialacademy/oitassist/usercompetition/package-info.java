@ApplicationModule(allowedDependencies = {"core::EntityRepository",
    "competition::CompetitionEntity", "competition::CompetitionStatus", "core::BaseService"})
package com.itasocialacademy.oitassist.usercompetition;

import org.springframework.modulith.ApplicationModule;