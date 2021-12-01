package ru.fbear.tcpchat.library

data class Message(
    val command: Command,
    val time: Long,
    val usernameLength: Int,
    val dataLength: Int,
    val username: List<Byte>,
    val data: List<Byte>
)
