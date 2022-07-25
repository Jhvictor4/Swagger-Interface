package com.jhvictor4.swaggerit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

const val BASE_PACKAGE_NAME = "com.jhvictor4.swaggerit"

@SpringBootApplication
class SwaggerInterfaceTrackerApplication

fun main(args: Array<String>) {
	runApplication<SwaggerInterfaceTrackerApplication>(*args)
}
