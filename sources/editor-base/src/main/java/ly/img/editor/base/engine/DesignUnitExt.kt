package ly.img.editor.base.engine

import ly.img.engine.DesignUnit

/** The unit a length field shows after its value. */
internal val DesignUnit.abbreviation: String
    get() = when (this) {
        DesignUnit.PIXEL -> "px"
        DesignUnit.MILLIMETER -> "mm"
        DesignUnit.INCH -> "in"
    }

/** An inch is large, so a hundredth is the precision that matters, as 0.25 in. `null` derives them from the step. */
internal val DesignUnit.lengthDecimals: Int?
    get() = if (this == DesignUnit.INCH) 2 else null
