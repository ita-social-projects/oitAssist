@ApplicationModule(allowedDependencies = {
        "core::EntityRepository",
        "competition::CompetitionEntity",
        "competition::CompetitionStatus",
        "core::BaseService",
        "core :: CreateEntityDTO",
        "core :: UpdateEntityDTO",
        "core :: AbstractRestControllerImpl",
        "core :: EntityDTO",
        "core :: AbstractServiceImpl",
        "core :: GeneralMapper",
        "core",
        "security :: SecurityFacade"
    })
package com.itasocialacademy.oitassist.usercompetition;

import org.springframework.modulith.ApplicationModule;