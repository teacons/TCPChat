package ru.fbear.tcpchat.server

import ru.fbear.tcpchat.library.*
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty()) Server(args[0].toInt()).launch()
    else println("Usage: java -jar Server.jar Port")
}

class Server(private val port: Int) {

    val charset = Charsets.UTF_8

    val authorizedSocketChannels = ConcurrentHashMap<SocketChannel, String>()

    private lateinit var serverSocketChannel: ServerSocketChannel

    val unauthorizedSocketChannel = mutableListOf<SocketChannel>()

    fun launch() {
        serverSocketChannel = ServerSocketChannel.open()

        serverSocketChannel.configureBlocking(false)

        serverSocketChannel.bind(InetSocketAddress(port))

        Thread(ConsoleReader()).start()
        Thread(ClientMessageHandler()).start()
        Thread(ClientAuthHandler()).start()

        while (serverSocketChannel.isOpen) {
            val socketChannel = serverSocketChannel.accept()
            if (socketChannel != null) {
                socketChannel.configureBlocking(false)
                unauthorizedSocketChannel.add(socketChannel)
            }
        }
    }

    inner class ConsoleReader : Runnable {

        override fun run() {
            while (true) {
                when (readLine()) {
                    "/stop", null -> {
                        println("Shutting down...")
                        disconnectAll()
                        serverSocketChannel.close()
                        println("Done")
                        exitProcess(0)
                    }
                    else -> println("Unknown command")
                }
            }
        }

    }

    inner class ClientAuthHandler : Runnable {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                val unauthorizedSocketChannelImmutable = unauthorizedSocketChannel.toList()
                for (socketChannel in unauthorizedSocketChannelImmutable) {
                    try {
                        val message = getMessage(socketChannel)

                        if (message == Message.empty()) continue

                        if (message.command == Command.CONNECT &&
                            authorizedSocketChannels.containsValue(
                                message.username.toByteArray().toString(charset).lowercase()
                            )
                        ) {
                            sendDisconnectCommand(socketChannel, "Username exists")
                            socketChannel.close()
                        } else {
                            sendMessage(
                                Message(
                                    Command.ACCEPT,
                                    System.currentTimeMillis() / 1000L,
                                    0,
                                    0,
                                    emptyList(),
                                    emptyList()
                                ),
                                socketChannel
                            )

                            val username = message.username.toByteArray().toString(charset)

                            authorizedSocketChannels[socketChannel] = username.lowercase()

                            unauthorizedSocketChannel.remove(socketChannel)

                            serverMessage(username, true)
                        }
                    } catch (e: IOException) {
                        sendDisconnectCommand(socketChannel, e.message)
                        socketChannel.close()
                    }
                }
            }
        }
    }

    inner class ClientMessageHandler : Runnable {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                for (socketChannel in authorizedSocketChannels.keys) {
                    try {
                        val message = getMessage(socketChannel)

                        if (message == Message.empty()) continue

                        if (message.command != Command.MESSAGE && message.command != Command.FILE) {
                            sendDisconnectCommand(socketChannel, "Received wrong command")
                            socketChannel.close()
                        }

                        if ((System.currentTimeMillis() / 1000L) - message.time > 60) {
                            sendDisconnectCommand(socketChannel, "Wrong time")
                            disconnectUser(socketChannel)
                        }

                        sendToAll(message)

                        println(
                            "<{${message.command}} ${getDateTime(message.time)}> [${
                                message.username.toByteArray().toString(charset)
                            }]"
                        )

                    } catch (e: IOException) {
                        disconnectUser(socketChannel)
                    }
                }
            }
        }
    }

    fun sendDisconnectCommand(socketChannel: SocketChannel, text: String?) {
        val data = text?.toByteArray(charset)?.toList() ?: "".toByteArray(charset).toList()

        sendMessage(
            Message(
                Command.DISCONNECT,
                System.currentTimeMillis() / 1000L,
                0,
                data.size,
                emptyList(),
                data
            ),
            socketChannel
        )
    }

    fun disconnectUser(socketChannel: SocketChannel) {
        socketChannel.close()
        val username = authorizedSocketChannels.remove(socketChannel)!!
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
        for (socketChannel in authorizedSocketChannels.keys) {
            sendDisconnectCommand(socketChannel, "Server shutting down")
            socketChannel.close()
        }
    }

    private fun sendToAll(message: Message) {
        for (socketChannel in authorizedSocketChannels.keys)
            try {
                sendMessage(message, socketChannel)
            } catch (e: IOException) {
                disconnectUser(socketChannel)
            }
    }

}



