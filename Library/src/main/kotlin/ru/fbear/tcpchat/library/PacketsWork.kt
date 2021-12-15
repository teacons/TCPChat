package ru.fbear.tcpchat.library

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.text.SimpleDateFormat
import java.util.*

fun getMessage(socketChannel: SocketChannel): Message {
    val firstByteBuffer = ByteBuffer.allocate(9).apply { clear() }
    val readBytesFirst = socketChannel.read(firstByteBuffer)

    if (readBytesFirst < 0) throw IOException("Stream end")
    if (readBytesFirst == 0) return Message.empty()

    val firstByteArray = firstByteBuffer.array()
    val command = when (firstByteArray[0].toUByte().toInt()) {
        0 -> Command.MESSAGE
        1 -> Command.CONNECT
        2 -> Command.ACCEPT
        3 -> Command.DISCONNECT
        4 -> Command.FILE
        else -> throw IllegalArgumentException("Wrong Command")
    }
    val time = firstByteArray.slice(1..4).toByteArray().toLong()
    val usernameLength = firstByteArray[5].toUByte().toInt()
    val dataLength = firstByteArray.slice(6..8).toByteArray().toInt()

    firstByteBuffer.flip()


    val usernameByteBuffer = ByteBuffer.allocate(usernameLength).apply { clear() }

    socketChannel.read(usernameByteBuffer)

    val username = usernameByteBuffer.array().toList()

    val dataByteList = mutableListOf<Byte>()

    var needRead = dataLength

    val dataByteBuffer = ByteBuffer.allocate(4096)

    while (needRead != 0) {
        dataByteBuffer.clear()
        val readBytesData = socketChannel.read(dataByteBuffer)
        dataByteList.addAll(dataByteBuffer.array().slice(0 until readBytesData))
        needRead -= readBytesData
        dataByteBuffer.flip()
    }


    return Message(
        command,
        time,
        usernameLength,
        dataLength,
        username,
        dataByteList.dropLastWhile { it == 0.toByte() })
}

fun sendMessage(message: Message, socketChannel: SocketChannel) {
    val dataLengthBytes = message.dataLength.toByteArray()
    val packet =
        byteArrayOf(message.command.byte.toByte()) +
                message.time.toByteArray() +
                byteArrayOf(message.usernameLength.toByte()) +
                dataLengthBytes + ByteArray(3 - dataLengthBytes.size) +
                message.username.toByteArray() +
                message.data.toByteArray()

    var written = 0

    while (written != packet.size) {
        val buffer = ByteBuffer.wrap(packet, written, packet.size - written)
        written += socketChannel.write(buffer)
    }

}

fun Long.toByteArray(): ByteArray {
    val bytes = mutableListOf<Byte>()

    var shift = 0

    var limit = this.countBits() / 8

    if (this.countBits() % 8 != 0) limit++

    for (i in 0 until limit) {
        bytes.add((this shr shift).toByte())
        shift += 8
    }

    return bytes.toByteArray()
}

fun Int.toByteArray() = this.toLong().toByteArray()


fun Long.countBits(): Int {
    var n = this
    var count = 0
    while (n != 0L) {
        count++
        n = n shr 1
    }
    return count
}

fun getDateTime(l: Long): String? {
    val sdf = SimpleDateFormat("HH:mm")
    val date = Date(l * 1000)
    return sdf.format(date)
}


fun ByteArray.toLong(): Long {
    var result = 0L
    var shift = 0
    this.forEach {
        result += it.toUByte().toLong() shl shift
        shift += 8
    }
    return result
}

fun ByteArray.toInt() = this.toLong().toInt()