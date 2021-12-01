package ru.fbear.tcpchat.server

import ru.fbear.tcpchat.library.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty()) Server(args[0].toInt()).launch()
    else println("Usage: java -jar Server.jar Port")
}

class Server(private val port: Int) {

    val charset = Charsets.UTF_8

    val clientSockets = mutableMapOf<Socket, String>()

    private lateinit var serverSocket: ServerSocket

    fun launch() {
        serverSocket = try {
            ServerSocket(port)
        } catch (e: IllegalArgumentException) {
            println("Wrong port")
            exitProcess(0)
        }

        Thread(ConsoleReader()).start()

        while (!serverSocket.isClosed) {
            val socket = serverSocket.accept()
            Thread(ClientMessageHandler(socket)).start()
        }
    }

    inner class ConsoleReader : Runnable {

        override fun run() {
            while (true) {
                when (readLine()) {
                    "/stop", null -> {
                        println("Shutting down...")
                        disconnectAll()
                        serverSocket.close()
                        println("Done")
                        exitProcess(0)
                    }
                    else -> println("Unknown command")
                }
            }
        }

    }

    inner class ClientMessageHandler(private val socket: Socket) : Runnable {

        private val inputStream = socket.getInputStream()

        private val outputStream = socket.getOutputStream()

        override fun run() {

            try {
                var message = getMessage(inputStream)

                if (message.command == Command.CONNECT &&
                    clientSockets.containsValue(message.username.toByteArray().toString(charset).lowercase())
                ) {
                    sendDisconnectCommand(socket, "Username exists")
                    socket.close()
                } else {
                    clientSockets[socket] = message.username.toByteArray().toString(charset).lowercase()

                    sendMessage(
                        Message(
                            Command.ACCEPT,
                            System.currentTimeMillis() / 1000L,
                            0,
                            0,
                            emptyList(),
                            emptyList()
                        ),
                        outputStream
                    )

                    serverMessage(clientSockets[socket]!!, true)
                    while (!socket.isClosed) {
                        message = getMessage(inputStream)
                        if ((System.currentTimeMillis() / 1000L) - message.time > 60) {
                            sendDisconnectCommand(socket, "Wrong time")
                            disconnectUser(socket)
                            Thread.currentThread().interrupt()
                        }
                        sendToAll(message)
                        println(
                            "<{${message.command}} ${getDateTime(message.time)}> [${
                                message.username.toByteArray().toString(charset)
                            }]"
                        )
                    }
                }
            } catch (e: SocketException) {
                disconnectUser(socket)
                Thread.currentThread().interrupt()
            }
        }

    }

    fun sendDisconnectCommand(socket: Socket, text: String) {
        val data = text.toByteArray(charset).toList()

        sendMessage(
            Message(
                Command.DISCONNECT,
                System.currentTimeMillis() / 1000L,
                0,
                data.size,
                emptyList(),
                data
            ),
            socket.getOutputStream()
        )
    }

    fun disconnectUser(socket: Socket) {
        socket.close()
        val username = clientSockets.remove(socket)!!
        serverMessage(username, false)

    }

    private fun serverMessage(username: String, connected: Boolean) {
        val serverUsername = "SERVER".toByteArray(charset).toList()
        val data = (if (connected) "Client $username connected to server"
        else "Client $username disconnected from server").toByteArray(charset).toList()
        sendToAll(
            Message(
                Command.MESSAGE,
                System.currentTimeMillis() / 1000L,
                serverUsername.size,
                data.size,
                serverUsername,
                data
            )
        )
    }

    private fun disconnectAll() {
        for (socket in clientSockets.keys)
            sendDisconnectCommand(socket, "Server shutting down")
    }

    private fun sendToAll(message: Message) {
        for (socket in clientSockets.keys)
            try {
                sendMessage(message, socket.getOutputStream())
            } catch (e: SocketException) {
                disconnectUser(socket)
            }
    }

}



