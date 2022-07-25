package com.jhvictor4.swaggerit.dto.response

import java.time.LocalDateTime

interface Response

data class ResponseImpl(
    val int: Int,
    val str: String,
): Response

data class ResponseRecursiveImpl(
    val timestamp: LocalDateTime,
    val response: ResponseImpl,
): Response
