@ApplicationModule(allowedDependencies = {
    "core",
    "competition::api",
    "competition::dto",
    "competition::exceptions",
    "competition :: enums",
    "submission :: api",
    "submission :: dto",
    "taskassignment :: api",
    "taskassignment :: dto",
    "security :: SecurityFacade",
    "filemanager :: api",
    "filemanager :: dto",
    "filemanager :: FileRole",
    "filemanager :: RelatedEntityType",
    "task :: dto",
    "task :: api"
})
package com.itasocialacademy.oitassist.evaluation;

import org.springframework.modulith.ApplicationModule;