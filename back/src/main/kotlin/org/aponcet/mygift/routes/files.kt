package org.aponcet.mygift.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.aponcet.mygift.model.Data
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private data class FileAnswer(val name: String)

fun Route.files(data: Data) {
    authenticate {
        route("/files") {
            post {
                var fileName = ""

                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val name = File(part.originalFileName!!).name
                            val ext = File(part.originalFileName!!).extension
                            fileName = "upload-${System.currentTimeMillis()}-${name.hashCode()}.$ext"
                            val file = File(data.uploads, fileName)
                            part.streamProvider().use { input ->
                                file.outputStream().buffered().use { output -> input.copyTo(output) }
                            }
                        }
                    }

                    part.dispose()
                }
                call.respond(HttpStatusCode.Accepted, FileAnswer(fileName))
            }
            get("/{name}") {
                //All the conversion part should be moved somewhere else
                val filename = call.parameters["name"]!!
                val tmpFile = File("${data.tmp}/$filename")
                if (tmpFile.exists()) {
                    call.respondFile(tmpFile)
                    return@get
                }

                val file = File("${data.uploads}/$filename")
                if (file.exists()) {
                    val resized = resize(file, 300.toDouble())
                    val output = File("${data.tmp}/$filename")
                    ImageIO.write(resized, tmpFile.extension, output)
                    call.respondFile(output)
                } else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

//will be squared resize, size being a side
private fun resize(file: File, size: Double): BufferedImage {
    val img = ImageIO.read(file)

    /** Keep proportion **/
    val oHeight = img.height.toDouble()
    val oWidth = img.width.toDouble()
    val scale: Double = if (oHeight < oWidth) oHeight / size else oWidth / size
    val width = (oWidth / scale).toInt()
    val height = (oHeight / scale).toInt()

    val resized = BufferedImage(
        width,
        height,
        if (img.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    )
    val g2d = resized.createGraphics()
    g2d.drawImage(img, 0, 0, width, height, null)
    g2d.dispose()
    return resized
}