package org.aponcet.mygift.dbmanager.maintenance

import org.aponcet.mygift.dbmanager.DbConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class CleanDataNotUsed(dbPath: String, uploadPath: String) {

    companion object {
        val LOGGER : Logger = LoggerFactory.getLogger(CleanDataNotUsed::class.java)
    }

    private val conn = DbConnection("sqlite", dbPath)
    private val uploadsFile = File(uploadPath)

    enum class DATA { PICTURES }

    fun execute(step: DATA) {
        when (step) {
            DATA.PICTURES -> cleanPictures()
        }
    }

    private fun cleanPictures() {
        /** Get pictures from DB **/
        val pictures = conn.safeExecute("SELECT picture FROM users", {
            with(it) {
                val res = executeQuery()
                val userPictures = HashSet<String>()
                while (res.next()) {
                    val element = res.getString("picture")
                    if (element.isNotBlank() && element.isNotEmpty()) userPictures.add(element)
                }
                return@with userPictures
            }
        }, "Execution of 'SELECT * FROM users' throw an exception")

        pictures.addAll(conn.safeExecute("SELECT picture FROM gifts", {
            with(it) {
                val res = executeQuery()
                val giftPictures = HashSet<String>()
                while (res.next()) {
                    val element = res.getString("picture")
                    if (element != null && element.isNotBlank() && element.isNotEmpty()) giftPictures.add(element)
                }
                return@with giftPictures
            }
        }, "Execution of 'SELECT * FROM gifts' throw an exception"))

        LOGGER.info("Pictures in DB: ${pictures.size} -> $pictures")

        val filesToKeep = ArrayList<String>()
        val filesToRemove = ArrayList<File>()
        uploadsFile.walk().forEach{file ->
            if (!file.isFile) return@forEach

            if (pictures.find{ file.name == it } == null) {
                filesToRemove.add(file.absoluteFile)
            } else {
                filesToKeep.add(file.name)
            }
        }

        LOGGER.info("Pictures in folder to keep/remove ${filesToKeep.size}/${filesToRemove.size}")
        LOGGER.info("Pictures in folder not in DB: $filesToRemove")

        val notDeleted = ArrayList<File>()
        var deleted = 0
        filesToRemove.forEach{
            if (it.delete()) deleted++ else notDeleted.add(it)
        }
        LOGGER.info("Report deleted/numberToDelete $deleted/${filesToRemove.size}")
        LOGGER.info("Report not deleted $notDeleted")
    }
}