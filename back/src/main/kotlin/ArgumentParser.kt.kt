import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import java.io.File

class ArgumentParser(parser: ArgParser) {
    val port : Int by parser.storing(
        "-p", "--port",
        help = "port used by the server") {
        toInt()
    }.default(8080)

    val db : String by parser.storing(
        "-d", "--database",
        help = "path to database")
        .default("mygift.db")
}