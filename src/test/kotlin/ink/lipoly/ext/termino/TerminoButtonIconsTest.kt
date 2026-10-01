package ink.lipoly.ext.termino

import com.intellij.util.IconUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class TerminoButtonIconsTest {
    @get:Rule val files = TemporaryFolder()

    @Test
    fun rendersLocalPngAndSvgAtToolbarSize() {
        val png = files.newFile("custom.png")
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color.GREEN
        graphics.fillRect(0, 0, 32, 32)
        graphics.dispose()
        ImageIO.write(image, "png", png)
        val svg = files.newFile("custom.svg")
        svg.writeText("""<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><rect width="32" height="32" fill="#ff0000"/></svg>""")

        val pngIcon = TerminoButtonIcons.loadCustom(png.path)
        val svgIcon = TerminoButtonIcons.loadCustom(svg.path)
        assertEquals(16, pngIcon.iconWidth)
        assertEquals(16, svgIcon.iconHeight)
        assertEquals(Color.GREEN.rgb, IconUtil.toBufferedImage(pngIcon).getRGB(8, 8))
        assertEquals(Color.RED.rgb, IconUtil.toBufferedImage(svgIcon).getRGB(8, 8))
    }

    @Test
    fun invalidCustomFileDoesNotReplaceSavedSettings() {
        val settings = TerminoSettings()
        settings.update("echo ready", ButtonIcon.PLAY, "")
        val invalid = files.newFile("broken.png")
        invalid.writeText("not a png")

        assertThrows(IllegalArgumentException::class.java) {
            settings.update("echo wrong", ButtonIcon.CUSTOM, invalid.path)
        }
        assertEquals("echo ready", settings.state.startupCommand)
        assertEquals(ButtonIcon.PLAY, settings.state.buttonIcon)
    }
}
