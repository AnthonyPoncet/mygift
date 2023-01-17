package org.aponcet.mygift

import com.lowagie.text.Document
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import java.io.FileOutputStream

class PdfGenerator {
}

fun main() {
    val document = Document(Rectangle(1440f, 809.3f), 60f,60f,53f,53f)

    val writer = PdfWriter.getInstance(document, FileOutputStream("test.pdf"))
    document.open()

    val images = arrayListOf("../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg", "../../Downloads/index.jpeg")

    /*val giftTables = ArrayList<Table>()
    for (imagePath in images) {
        val giftTable = Table(1, 4)

        val image = Image.getInstance(imagePath)
        image.scaleAbsolute(205f, 282f)
        val imageCell = Cell(image)
        imageCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
        imageCell.border = 0

        val shopCell = Cell(Paragraph("Magasin"))
        shopCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
        shopCell.border = 0

        val titleCell = Cell(Paragraph("Title"))
        titleCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
        titleCell.border = 0

        giftTable.addCell(imageCell)
        giftTable.addCell(shopCell)
        giftTable.addCell(titleCell)

        giftTables.add(giftTable)
    }


    val table = Table(5, 2)
    table.setHorizontalAlignment(HorizontalAlignment.LEFT)
    table.width = 100f
    //table.padding = 5f
    table.spacing = 2f
    for (giftTable in giftTables) {
        val tableCell = Cell(giftTable)
        tableCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
        tableCell.border = 0
        table.addCell(tableCell)
    }
    document.add(table)*/


    val table = Table(5, 2)
    table.border = 0
    table.width = 100f
    table.padding = 5f
    table.spacing = 5f
    for (imagePath in images) {
        val image = Image.getInstance(imagePath)
        image.scaleAbsolute(205f, 282f)
        val cell = Cell(image)
        cell.setHorizontalAlignment(HorizontalAlignment.CENTER)
        cell.border = 0

        cell.add(Paragraph("Magasin"))
        cell.add(Paragraph("Title"))

        table.addCell(cell)
    }
    document.add(table)

    val cb = writer.directContent
    cb.setRGBColorFill(255, 255, 255)
    cb.circle(265f, 717f, 20f)
    cb.circle(530f, 717f, 20f)
    cb.circle(795f, 717f, 20f)
    cb.circle(1060f, 717f, 20f)
    cb.circle(1325f, 717f, 20f)
    cb.circle(265f, 375f, 20f)
    cb.circle(530f, 375f, 20f)
    cb.circle(795f, 375f, 20f)
    cb.circle(1060f, 375f, 20f)
    cb.circle(1325f, 375f, 20f)
    cb.fill()

    val bf = BaseFont.createFont()
    cb.setRGBColorFill(0, 0, 0)
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(265f-7f, 714f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(530f-7f, 714f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(795f-7f, 714f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(1060f-7f, 714f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(1325f-7f, 714f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(265f-7f, 372f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(530f-7f, 372f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(795f-7f, 372f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(1060f-7f, 372f)
    cb.showText("50€")
    cb.endText()
    cb.beginText()
    cb.setFontAndSize(bf, 10f)
    cb.moveText(1325f-7f, 372f)
    cb.showText("50€")
    cb.endText()
    cb.fillStroke()

    document.close()
}