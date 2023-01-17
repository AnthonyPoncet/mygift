package org.aponcet.mygift

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.categoryFont
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.categoryFontHeight
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.CATEGORY_FONT_SIZE
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.giftFont
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.GIFT_FONT_SIZE
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.GIFT_HEIGHT
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.GIFT_LEADING
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.GIFT_WIDTH
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.giftFontHeight
import org.aponcet.mygift.PdfGenerator.PdfGeneratorConstant.GIFT_MARGIN_TOP
import org.aponcet.mygift.dbmanager.DatabaseManager
import org.aponcet.mygift.model.AuthServer
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO


class PdfGenerator(private val uploadRootPath: String) {
    object PdfGeneratorConstant {
        val categoryFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        const val CATEGORY_FONT_SIZE = 50f
        val categoryFontHeight = categoryFont.fontDescriptor.getFontBoundingBox().height / 1000 * CATEGORY_FONT_SIZE
        val giftFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        const val GIFT_FONT_SIZE = 12f
        val giftFontHeight = giftFont.fontDescriptor.getFontBoundingBox().height / 1000 * GIFT_FONT_SIZE
        const val GIFT_MARGIN_TOP = 5f
        const val GIFT_LEADING = 2f
        const val GIFT_WIDTH = 205f
        const val GIFT_HEIGHT = 282f
    }

    private fun centerText(text: String, font: PDFont, fontSize: Float, width: Float): Float {
        val titleWidth = font.getStringWidth(text) / 1000 * fontSize
        return (width - titleWidth) / 2
    }

    fun generateDoc(categoriesAndGifts: List<CatAndGift>, output: OutputStream) {
        val document = PDDocument()

        for (categoryAndGifts in categoriesAndGifts) {
            var page = newPage(document, categoryAndGifts.category)
            val positions = arrayListOf(
                Pair(89.5f, 429.3f),
                Pair(353.5f, 429.3f),
                Pair(617.5f, 429.3f),
                Pair(881.5f, 429.3f),
                Pair(1145.5f, 429.3f),
            )

            var count = 0
            for (gift in categoryAndGifts.gifts) {
                var picture = gift.picture
                if (picture == null || picture == "" || picture.endsWith("svg")) {
                    picture = "blank_gift.png"
                }

                //Resize
                val file = File("$uploadRootPath/$picture")
                val img = ImageIO.read(file)
                /** Keep proportion **/
                val oHeight = img.height.toDouble()
                val oWidth = img.width.toDouble()
                val scale: Double = if (oHeight/GIFT_HEIGHT > oWidth/GIFT_WIDTH) oHeight / GIFT_HEIGHT else oWidth / GIFT_WIDTH
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
                val pdImage: PDImageXObject = LosslessFactory.createFromImage(document, resized)
                val contentStream = PDPageContentStream(document, page, AppendMode.APPEND, true, true)
                val position = positions[count]
                contentStream.drawImage(pdImage, position.first + (GIFT_WIDTH-width)/2, position.second + (GIFT_HEIGHT-height)/2, width.toFloat(), height.toFloat())

                //Border
                contentStream.setStrokingColor(Color.BLACK)
                contentStream.addRect(position.first-3, position.second-300, GIFT_WIDTH + 6, GIFT_HEIGHT + 306)
                contentStream.closeAndStroke()

                contentStream.beginText()
                contentStream.setFont(giftFont, GIFT_FONT_SIZE)
                horizontalCenterText(gift.name, contentStream, position.first, position.second)
                contentStream.endText()

                if (gift.description != null) {
                    contentStream.beginText()
                    contentStream.setFont(giftFont, GIFT_FONT_SIZE)
                    horizontalCenterText(gift.description, contentStream, position.first, position.second - 4*(giftFontHeight+GIFT_MARGIN_TOP))
                    contentStream.endText()
                }

                //Price circle
                if (!gift.price.isNullOrEmpty()) {
                    contentStream.setNonStrokingColor(Color.LIGHT_GRAY)
                    val rayon = 25f
                    val k = 0.5522848f;
                    val centerX = position.first + GIFT_WIDTH - 52f + rayon
                    val centerY = position.second + GIFT_HEIGHT - 52f + rayon
                    contentStream.moveTo(centerX - rayon, centerY)
                    contentStream.curveTo(
                        centerX - rayon,
                        centerY + k * rayon,
                        centerX - k * rayon,
                        centerY + rayon,
                        centerX,
                        centerY + rayon
                    )
                    contentStream.curveTo(
                        centerX + k * rayon,
                        centerY + rayon,
                        centerX + rayon,
                        centerY + k * rayon,
                        centerX + rayon,
                        centerY
                    )
                    contentStream.curveTo(
                        centerX + rayon,
                        centerY - k * rayon,
                        centerX + k * rayon,
                        centerY - rayon,
                        centerX,
                        centerY - rayon
                    )
                    contentStream.curveTo(
                        centerX - k * rayon,
                        centerY - rayon,
                        centerX - rayon,
                        centerY - k * rayon,
                        centerX - rayon,
                        centerY
                    )
                    contentStream.fill()

                    contentStream.beginText()
                    contentStream.setFont(giftFont, GIFT_FONT_SIZE)
                    contentStream.setNonStrokingColor(Color.BLACK)
                    contentStream.newLineAtOffset(
                        position.first + GIFT_WIDTH - 2 * rayon + centerText(
                            gift.price,
                            giftFont,
                            GIFT_FONT_SIZE,
                            2 * rayon
                        ) - 2f, position.second + GIFT_HEIGHT - rayon - GIFT_FONT_SIZE / 2
                    )
                    contentStream.showText(gift.price)
                    contentStream.endText()
                }


                contentStream.close()

                // Hyperlink on the image
                if (gift.whereToBuy != null) {
                    val annotation = PDAnnotationLink()
                    val annotationPosition = PDRectangle()
                    annotationPosition.lowerLeftX = position.first
                    annotationPosition.lowerLeftY = position.second
                    annotationPosition.upperRightX = position.first + GIFT_WIDTH
                    annotationPosition.upperRightY = position.second + GIFT_HEIGHT
                    annotation.rectangle = annotationPosition
                    val action = PDActionURI()
                    action.uri = gift.whereToBuy
                    annotation.action = action
                    page.annotations.add(annotation)
                }

                if (gift.description != null) {
                    val annotation = PDAnnotationPopup()
                    val annotationPosition = PDRectangle()
                    annotationPosition.lowerLeftX = position.first
                    annotationPosition.upperRightY = position.second - (giftFontHeight + GIFT_MARGIN_TOP)
                    annotationPosition.upperRightX = position.first + GIFT_WIDTH
                    annotationPosition.lowerLeftY = position.second
                    annotation.rectangle = annotationPosition
                    annotation.contents = gift.description
                }

                count += 1
                if (count == 5) {
                    document.addPage(page)
                    page = newPage(document, categoryAndGifts.category)
                    count = 0
                }
            }

            document.addPage(page)
        }

        document.save(output)
        document.close()
    }

    private fun newPage(document: PDDocument, category: CleanCategory): PDPage {
        val page = PDPage(PDRectangle(1440f, 809.3f))
        val contentStream = PDPageContentStream(document, page)
        contentStream.beginText()
        contentStream.setFont(categoryFont, CATEGORY_FONT_SIZE)
        contentStream.newLineAtOffset(
            centerText(
                category.name,
                categoryFont,
                CATEGORY_FONT_SIZE,
                page.getMediaBox().width
            ), page.getMediaBox().height - categoryFontHeight
        );
        contentStream.showText(category.name)
        contentStream.endText()
        contentStream.close()

        return page
    }

    private fun horizontalCenterText(originalText: String, contentStream: PDPageContentStream, initX: Float, initY: Float) {
        var text = originalText.replace("\n", " ").replace("\r", "").trim()
        //text = String(text.toByteArray(Charsets.ISO_8859_1))
        println("----------")
        println(text)
        var remainingText = ""
        var centerPosition = centerText(text, giftFont, GIFT_FONT_SIZE, GIFT_WIDTH)
        while (centerPosition < 0) {
            val lastSpace = text.lastIndexOf(" ")
            if (lastSpace == -1) {
                //text is too long, drop 1 (not efficient)
                remainingText = text.substring(text.length - 1) + remainingText
                text = text.dropLast(1)
                println(remainingText)
                println(text)
            }
            remainingText = text.substring(lastSpace + 1) + " " + remainingText
            text = text.dropLast(text.length - lastSpace)
            centerPosition = centerText(text, giftFont, GIFT_FONT_SIZE, GIFT_WIDTH)
        }
        contentStream.newLineAtOffset(
            initX + centerPosition,
            initY - (giftFontHeight + GIFT_MARGIN_TOP)
        )
        contentStream.showText(text)

        var previousPosition = centerPosition
        while (remainingText.isNotEmpty()) {
            text = remainingText.trim()
            remainingText = ""
            centerPosition = centerText(text, giftFont, GIFT_FONT_SIZE, GIFT_WIDTH)
            while (centerPosition < 0) {
                val lastSpace = text.lastIndexOf(" ")
                if (lastSpace == -1) {
                    //text is too long, drop 1 (not efficient)
                    remainingText = text.substring(text.length - 1) + remainingText
                    text = text.dropLast(1)
                    println(remainingText)
                    println(text)
                } else {
                    remainingText = text.substring(lastSpace + 1) + " " + remainingText
                    text = text.dropLast(text.length - lastSpace)
                }
                centerPosition = centerText(text, giftFont, GIFT_FONT_SIZE, GIFT_WIDTH)
            }
            contentStream.newLineAtOffset(
                -previousPosition + centerPosition,
                -(giftFontHeight + GIFT_LEADING)
            )
            contentStream.showText(text)
            previousPosition = centerPosition
        }
    }
}

fun main() {
    val databaseManager = DatabaseManager("test-files\\23_11_04.db")
    val userManager = UserManager(databaseManager, AuthServer("", 0))
    val categoriesAndGifts = userManager.getUserGifts(21)

    val output = FileOutputStream("test2.pdf")
    val generator = PdfGenerator("test-files\\uploads")
    generator.generateDoc(categoriesAndGifts, output)

    output.flush()
    output.close()
}