package ly.img.editor.base

import androidx.core.content.FileProvider

/**
 * Custom FileProvider for the IMG.LY Editor SDK.
 *
 * This class extends [FileProvider] to provide a unique class name that avoids
 * manifest merger conflicts when integrating with other libraries that also use FileProvider.
 */
class ImglyEditorFileProvider : FileProvider()
