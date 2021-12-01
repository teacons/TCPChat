package ru.fbear.tcpchat.library

enum class Command(val byte: Int) {
    MESSAGE(0),
    CONNECT(1),
    ACCEPT(2),
    DISCONNECT(3),
    FILE(4)
}