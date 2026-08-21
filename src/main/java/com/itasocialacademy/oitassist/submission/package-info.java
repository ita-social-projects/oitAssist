@ApplicationModule(
    displayName = "Task",
    allowedDependencies = {"core", "security :: SecurityFacade", "filemanager :: api",})
package com.itasocialacademy.oitassist.submission;

import org.springframework.modulith.ApplicationModule;