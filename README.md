# Termino

> [!CAUTION]
> <img src="https://lipoly.ink/assets/badges/llmWarn.svg" alt="This project contains unaudited AI generated code" width="200">
>
> 该项目暂为本人自用, 代码审查不完善, 不保证功能一切正常

IntelliJ Platform 编辑器内终端插件。IDE 主工具栏右侧提供一个按钮，每次点击都会在编辑器区域新增独立的终端标签页。

关闭标签页会结束对应的终端会话。把标签拖到其他编辑器窗口或分屏时，IDE 会先关闭再在同一文件上重建编辑器，此时终端会话保持运行，可在新窗口中继续使用。编辑器内使用 IntelliJ IDEA 2026.2 的 Reworked Terminal 组件；终端虚拟文件不会落盘。

在 `Settings > Tools > Termino` 可设置「启动指令」和主工具栏按钮图标：选择内置「终端 / 运行 / Oh My Pi（渐变）/ Oh My Pi（单色）」预设，或选择「自定义图标」后点击「浏览…」选取本地 SVG/PNG 文件（推荐 16 × 16），右侧实时预览。设置保存在整个 IDE 中；指令仅在之后新建的每个终端中执行一次，空白指令不执行，已打开的标签不会补执行。自定义图标的路径保存在本机；图片不存在或无效时不能应用，之前保存的图标不受影响。如果重启后图片已被移走，按钮临时使用默认图标，设置页仍保留原路径供重新选择。

终端编辑器标签的图标与主工具栏按钮使用相同设置；更换图标并应用后，已有终端标签和之后新建的标签都会显示新图标。

编辑器标签标题及内存 VirtualFile 的文件名同步于对应终端会话显示的标题，遵循 IDE Terminal 的应用程序标题显示设置；终端内程序发出的 OSC 0/2 标题是否显示取决于该设置，文件名不会反向写入 shell。

## 运行

在 Windows 执行 `gradlew.bat runIde`，在 macOS / Linux 执行 `./gradlew runIde`（目标版本：IntelliJ IDEA 2026.2.3）。在启动的开发版 IDE 中打开一个项目，在 `Settings > Tools > Termino` 配置终端后点击顶栏终端图标。

## 编译

在 Windows 执行 `gradlew.bat compileKotlin processResources`，在 macOS / Linux 执行 `./gradlew compileKotlin processResources`。插件声明位于 `src/main/resources/META-INF/plugin.xml`；终端编辑器实现位于 `src/main/kotlin/ink/lipoly/ext/termino/EditorTerminal.kt`。
