package com.vakitniyet.common.util

import org.slf4j.MDC
import java.util.UUID

object TraceContext {
    fun newTrace(): String {
        val traceId = UUID.randomUUID().toString().take(8)
        MDC.put("traceId", traceId)
        return traceId
    }

    fun clear() = MDC.clear()
}
