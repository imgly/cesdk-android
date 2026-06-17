package ly.img.camera

/**
 * Drives shutter routing while `CaptureType.Mixed` is active. Ignored for pure `Photo`/`Video`
 * capture types, where the shutter behavior is fully determined by `CaptureType` itself.
 */
internal enum class ActiveMixedSubMode {
    Photo,
    Video,
}
