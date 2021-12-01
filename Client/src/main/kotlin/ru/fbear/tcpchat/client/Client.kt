package ru.fbear.tcpchat.client

import ru.fbear.tcpchat.library.*
import java.io.File
import java.io.IOException
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.file.Paths
import kotlin.system.exitProcess

class Client(private val host: String, private val port: Int) {

    val charset = Charsets.UTF_8

    fun launch() {
        var username: String? = null
        while (username.isNullOrEmpty()) {
            print("Enter your username: ")
            val text = readLine()
            when {
                text.isNullOrEmpty() -> println("Wrong username")
                text.length > 10 -> println("Wrong username")
                text.isNotEmpty() -> username = text
            }
        }

        val socket =
            try {
                println("Connecting to server...")
                Socket(host, port)
            } catch (e: IOException) {
                println("Not connected")
                exitProcess(0)
            } catch (e: UnknownHostException) {
                println("Not connected")
                println("Host unreachable")
                exitProcess(0)
            }

        val usernameBytes = username.toByteArray(charset)


        try {
            sendMessage(
                Message(
                    Command.CONNECT,
                    System.currentTimeMillis() / 1000L,
                    usernameBytes.size,
                    0,
                    usernameBytes.toList(),
                    emptyList()
                ),
                socket.getOutputStream()
            )
        } catch (e: SocketException) {
            socket.close()
            println("Not connected")
            println("Reason: ${e.message}")
            exitProcess(0)
        }

        Thread(MessageHandler(socket, username)).start()

        while (!socket.isClosed) {
            val text = readLine()

            when {
                text == null || text == "/stop" -> {
                    println("Shutting down...")
                    socket.close()
                    println("Done")
                    exitProcess(0)
                }
                text.startsWith("/file") -> {
                    val filepath = text.removePrefix("/file ")
                    val file = File(filepath)
                    if (file.exists()) {
                        val fileBytes = File(filepath).readBytes()
                        if (fileBytes.size <= 16777215) {
                            try {
                                sendMessage(
                                    Message(
                                        Command.FILE,
                                        System.currentTimeMillis() / 1000L,
                                        usernameBytes.size,
                                        fileBytes.size,
                                        usernameBytes.toList(),
                                        fileBytes.toList()
                                    ),
                                    socket.getOutputStream()
                                )
                                println("File sent")
                            } catch (e: SocketException) {
                                socket.close()
                                println("Disconnected from server")
                                println("Reason: ${e.message}")
                                exitProcess(0)
                            }
                        } else println("Big file")
                    } else println("File not found")
                }
                else ->
                    if (text.isNotEmpty()) {
                        val data = text.toByteArray(charset).toList()
                        try {
                            sendMessage(
                                Message(
                                    Command.MESSAGE,
                                    System.currentTimeMillis() / 1000L,
                                    usernameBytes.size,
                                    data.size,
                                    usernameBytes.toList(),
                                    data
                                ),
                                socket.getOutputStream()
                            )
                        } catch (e: SocketException) {
                            socket.close()
                            println("Disconnected from server")
                            println("Reason: ${e.message}")
                            exitProcess(0)
                        }
                    }
            }
        }
    }

    inner class MessageHandler(private val socket: Socket, private val username: String) : Runnable {

        private val inputStream = socket.getInputStream()

        override fun run() {
            try {
                while (!socket.isClosed) {
                    val message = getMessage(inputStream)

                    when (message.command) {
                        Command.MESSAGE -> println(
                            "<${getDateTime(message.time)}> [${message.username.toByteArray().toString(charset)}] ${
                                message.data.toByteArray().toString(charset)
                            }"
                        )
                        Command.ACCEPT -> {
                            println("Success")
                        }
                        Command.DISCONNECT -> {
                            socket.close()
                            println("Disconnected from server")
                            if (message.data.isNotEmpty()) println(
                                "Reason: ${
                                    message.data.toByteArray().toString(charset)
                                }"
                            )
                            exitProcess(0)
                        }
                        Command.FILE -> {
                            if (message.username.toByteArray().toString(charset) != username) {
                                val path = Paths.get("").toAbsolutePath().toString() + File.separator + "file"
                                val file = File(path).apply { writeBytes(message.data.toByteArray()) }
                                println("Received file from ${message.username.toByteArray().toString(charset)}")
                                println(file.absolutePath)
                            }
                        }
                        else -> {
                            throw IllegalArgumentException("Wrong Command")
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                println("Received wrong command from server")
            } catch (e: SocketException) {
                socket.close()
                println("Disconnected from server")
                println("Reason: ${e.message}")
                exitProcess(0)
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) Client(args[0], args[1].toInt()).launch()
    else println("Usage: java -jar Client.jar Host Port")

}
