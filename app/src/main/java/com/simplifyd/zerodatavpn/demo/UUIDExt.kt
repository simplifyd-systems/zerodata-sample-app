package com.simplifyd.zerodatavpn.demo

import android.util.Base64
import java.nio.ByteBuffer
import java.util.UUID

fun String.readIntoByteArray(): ByteArray {
    return this.let {
        Base64.decode(this, Base64.DEFAULT)
    }
}

fun ByteArray.toUUID(): UUID {
    val bb: ByteBuffer = ByteBuffer.wrap(this)
    return UUID(bb.long, bb.long)
}