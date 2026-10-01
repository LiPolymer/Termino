package ink.lipoly.ext.termino

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.util.SVGLoader
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon

/** Built-in presets and a local image chosen by the user. */
enum class ButtonIcon {
    TERMINAL,
    PLAY,
    OH_MY_PI_GRADIENT,
    OH_MY_PI_MONO,
    CUSTOM,
}

@Service(Service.Level.APP)
@State(name = "TerminoSettings", storages = [Storage("Termino.xml", roamingType = RoamingType.DISABLED)])
class TerminoSettings : PersistentStateComponent<TerminoSettings.Values> {
    data class Values(
        var startupCommand: String = "",
        var buttonIcon: ButtonIcon = ButtonIcon.TERMINAL,
        var customIconPath: String = "",
    )

    private data class Resolved(val values: Values, val icon: Icon)

    @Volatile
    private var current = Resolved(Values(), TerminoButtonIcons.forOption(ButtonIcon.TERMINAL))

    override fun getState(): Values = current.values

    override fun loadState(state: Values) {
        val icon = try {
            TerminoButtonIcons.resolve(state.buttonIcon, state.customIconPath)
        } catch (_: Exception) {
            TerminoButtonIcons.forOption(ButtonIcon.TERMINAL)
        }
        current = Resolved(state, icon)
    }

    fun update(startupCommand: String, buttonIcon: ButtonIcon, customIconPath: String) {
        val icon = TerminoButtonIcons.resolve(buttonIcon, customIconPath)
        current = Resolved(Values(startupCommand, buttonIcon, customIconPath), icon)
    }

    fun toolbarIcon(): Icon = current.icon

    companion object {
        fun getInstance(): TerminoSettings = ApplicationManager.getApplication().getService(TerminoSettings::class.java)
    }
}

internal object TerminoButtonIcons {
    private val terminal = IconLoader.getIcon("/icons/terminal.svg", TerminoButtonIcons::class.java)
    private val play = IconLoader.getIcon("/icons/terminalPlay.svg", TerminoButtonIcons::class.java)
    private val ohMyPiGradient = IconLoader.getIcon("/icons/ohMyPiGradient.svg", TerminoButtonIcons::class.java)
    private val ohMyPiMono = IconLoader.getIcon("/icons/ohMyPiMono.svg", TerminoButtonIcons::class.java)

    fun forOption(option: ButtonIcon): Icon = when (option) {
        ButtonIcon.TERMINAL -> terminal
        ButtonIcon.PLAY -> play
        ButtonIcon.OH_MY_PI_GRADIENT -> ohMyPiGradient
        ButtonIcon.OH_MY_PI_MONO -> ohMyPiMono
        ButtonIcon.CUSTOM -> terminal
    }

    fun resolve(option: ButtonIcon, customIconPath: String): Icon =
        if (option == ButtonIcon.CUSTOM) loadCustom(customIconPath) else forOption(option)

    fun loadCustom(path: String): Icon {
        val file = File(path)
        require(file.isFile && file.canRead()) { "图标文件不存在或不可读" }
        val extension = file.extension.lowercase()
        require(extension == "svg" || extension == "png") { "仅支持 SVG 或 PNG 图标" }
        val url = file.toURI().toURL()
        if (extension == "svg") {
            SVGLoader.load(url, 1f)
        } else {
            require(ImageIO.read(file) != null) { "PNG 图标无法解码" }
        }
        val icon = IconLoader.findIcon(url) ?: throw IllegalArgumentException("图标无法加载")
        return IconUtil.toSize(icon, 16, 16)
    }
}
