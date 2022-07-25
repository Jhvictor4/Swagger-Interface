package com.jhvictor4.swaggerit.controller

import com.jhvictor4.swaggerit.dto.request.Request
import com.jhvictor4.swaggerit.dto.response.Response
import com.jhvictor4.swaggerit.dto.response.ResponseImpl
import com.jhvictor4.swaggerit.dto.response.ResponseRecursiveImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class BaseController {
    @GetMapping("/")
    fun request(
        request: Request
    ): Response {
        return listOf(
            ResponseImpl(1, "a"),
            ResponseRecursiveImpl(LocalDateTime.MAX, ResponseImpl(1, "b")),
        ).random()
    }
}
