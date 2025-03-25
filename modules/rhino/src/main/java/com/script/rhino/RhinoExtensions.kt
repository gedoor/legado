package com.script.rhino

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.mozilla.javascript.Context
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@Suppress("LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
inline fun <T> suspendContinuation(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val cx = Context.enter()
    try {
        val pending = cx.captureContinuation()
        pending.applicationState = suspend {
            supervisorScope {
                block()
            }
        }
        throw pending
    } catch (e: IllegalStateException) {
        return runBlocking { block() }
    } finally {
        Context.exit()
    }
}

inline fun <T> runScriptWithContext(context: CoroutineContext, block: () -> T): T {
    RhinoScriptEngine
    val rhinoContext = Context.enter() as RhinoContext
    val previousCoroutineContext = rhinoContext.coroutineContext
    rhinoContext.coroutineContext = context.minusKey(ContinuationInterceptor)
    try {
        return block()
    } finally {
        rhinoContext.coroutineContext = previousCoroutineContext
        Context.exit()
    }
}

suspend inline fun <T> runScriptWithContext(block: () -> T): T {
    val rhinoContext = Context.enter() as RhinoContext
    val previousCoroutineContext = rhinoContext.coroutineContext
    rhinoContext.coroutineContext = coroutineContext.minusKey(ContinuationInterceptor)
    try {
        return block()
    } finally {
        rhinoContext.coroutineContext = previousCoroutineContext
        Context.exit()
    }
}
