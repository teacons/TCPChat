package ru.fbear.tcpchat.library

import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.*

fun getMessage(inputStream: InputStream): Message {
    val firstHalfOfPacket = ByteArray(9)
    val readBytesFirstHalfPacket = inputStream.read(firstHalfOfPacket)

    if (readBytesFirstHalfPacket == -1) throw SocketException("Stream end")

    val command = when (firstHalfOfPacket[0].toUByte().toInt()) {
        0 -> Command.MESSAGE
        1 -> Command.CONNECT
        2 -> Command.ACCEPT
        3 -> Command.DISCONNECT
        4 -> Command.FILE
        else -> throw IllegalArgumentException("Wrong Command")
    }
    val time = firstHalfOfPacket.slice(1..4).toByteArray().toLong()
    val usernameLength = firstHalfOfPacket[5].toUByte().toInt()
    val dataLength = firstHalfOfPacket.slice(6..8).toByteArray().toInt()

    val username = ByteArray(usernameLength)

    inputStream.read(username)

    val dataByteList = mutableListOf<Byte>()

    var needRead = dataLength

    while (needRead != 0) {
        val dataByteArray = ByteArray(4096)
        val readBytesDataByteArray = inputStream.read(dataByteArray)
        dataByteList.addAll(dataByteArray.slice(0 until readBytesDataByteArray))
        needRead -= readBytesDataByteArray
    }

    return Message(
        command,
        time,
        usernameLength,
        dataLength,
        username.toList(),
        dataByteList
    )
}

fun sendMessage(message: Message, outputStream: OutputStream) {
    val dataLengthBytes = message.dataLength.toByteArray()
    val packet =
        byteArrayOf(message.command.byte.toByte()) +
                message.time.toByteArray() +
                byteArrayOf(message.usernameLength.toByte()) +
                dataLengthBytes + ByteArray(3 - dataLengthBytes.size) +
                message.username.toByteArray() +
                message.data.toByteArray()

    outputStream.write(packet)
    outputStream.flush()
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