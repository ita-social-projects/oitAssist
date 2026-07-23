@ApplicationModule(
    displayName = "Task Assignment",
    allowedDependencies = {"core", "competition :: exceptions", "competition :: api", "competition :: dto",
        "task :: exceptions", "task :: dto", "task :: api"})
package com.itasocialacademy.oitassist.taskassignment;

import org.springframework.modulith.ApplicationModule;