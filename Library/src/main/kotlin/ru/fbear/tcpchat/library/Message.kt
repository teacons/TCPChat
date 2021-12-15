package ru.fbear.tcpchat.library

data class Message(
    val command: Command,
    val time: Long,
    val usernameLength: Int,
    val dataLength: Int,
    val username: List<Byte>,
    val data: List<Byte>
) {
    companion object {
        fun empty(): Message {
            return Message(Command.MESSAGE, 0L, 0, 0, emptyList(), emptyList())
        }
    }
}
