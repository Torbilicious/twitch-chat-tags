package de.torbilicious

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.Calendar
import java.util.Date


private val socket = Socket("irc.chat.twitch.tv", 6667)
private val out = PrintWriter(socket.getOutputStream())
private val inStream = BufferedReader(InputStreamReader(socket.getInputStream()))

private val messageRegEx = "^:(\\w+)!\\w+@\\w+\\.tmi\\.twitch\\.tv (PRIVMSG) #\\w+(?: :(.*))?\$".toRegex()

data class Message(
    val date: Date,
    val text: String
)

private val messages = mutableListOf<Message>()

fun main() {
    joinChannel("method")

    var running = true
    Thread {
        while (running) {
            messages.removeAll {
                val oneMinuteAgo = Calendar.getInstance().apply { add(Calendar.MINUTE, -1) }.time
                it.date.before(oneMinuteAgo)
            }

            println("Messages since one minute ago: ${messages.count()}")

            val tags = messages.map { it.text.split(" ") }.flatten()
            val stuff = tags.groupBy { it }.values.sortedBy { it.size }.takeLast(5)
            stuff.forEach {
                println("#${it.size} ${it.first()}")
            }

            Thread.sleep(100)
        }
    }.start()

    while (running) {
        val line = inStream.readLine() ?: return
//        println(line)

        when {
            line == "PING :tmi.twitch.tv" -> {
                println("Awnsering ping.")
                out.print("PONG :tmi.twitch.tv\r\n")

            }

            line.matches(messageRegEx) -> {
                val result = messageRegEx.matchEntire(line)

                if (result != null) {
                    val name = result.groups[1]!!.value
                    val message = result.groups[3]!!.value

                    messages.add(Message(Date(), message))

//                    println("$name: $message")
                }

            }
        }

        Thread.sleep(50)
    }

    close()
}

private fun close() {
    inStream.close()
    out.close()
    socket.close()
}

private fun joinChannel(channel: String) {
    val password = System.getenv("TWITCH_TOKEN")
    val name = "TwitchBot"

    require(password != null)

    out.write("PASS $password\r\n")
    out.write("NICK $name\r\n")
    out.write("JOIN #$channel\r\n")

    out.flush()
}
