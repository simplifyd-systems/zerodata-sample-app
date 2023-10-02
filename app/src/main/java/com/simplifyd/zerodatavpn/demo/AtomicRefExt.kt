package com.simplifyd.zerodatavpn.demo

import java.util.concurrent.atomic.AtomicReference

fun <V> AtomicReference<V>.getOrNull(): V? {
    return if (get() != null) {
        get()
    } else {
        null
    }
}

fun <V> AtomicReference<V>.getOrDefault(default: V): V {
    return if (get() != null) {
        get()
    } else {
        default
    }
}