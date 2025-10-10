# `editor-core` depends upon `camera-core` as `compileOnly`.
# Without adding the below rule, consumers of `editor-core` get a `Missing classes detected while running R8.` error if they don't include the camera module.
-dontwarn ly.img.camera.core.**

# Preserve gallery asset source implementation that is instantiated via Engine APIs.
-keep class ly.img.editor.core.library.data.SystemGalleryAssetSource { *; }
-keep class ly.img.editor.core.library.data.SystemGalleryAssetSourceType { *; }
-keep class ly.img.editor.core.library.data.GalleryPermissionManager { *; }
-keep class ly.img.editor.core.library.data.SystemGalleryThumbnailUris { *; }
