package com.photobooth.data.model

/**
 * Immutable data model representing all user-configurable app settings.
 * Follows Single Responsibility – only holds configuration state.
 */
data class AppSettings(
    // ── Appearance ─────────────────────────────────────────────
    val selectedTheme: AppTheme = AppTheme.DARK_GOLD,
    val appLanguage: AppLanguage = AppLanguage.HEBREW,
    val selectedFrame: PhotoFrame = PhotoFrame.NONE,

    // ── Camera ─────────────────────────────────────────────────
    val useFrontCamera: Boolean = true,
    val externalCameraEnabled: Boolean = false,

    // ── Session ────────────────────────────────────────────────
    val countdownSeconds: Int = 3,
    val photosPerSession: Int = 3,       // Always 3 – vertical strip
    val delayBetweenPhotos: Int = 4,

    // ── Printer ────────────────────────────────────────────────
    val printerEnabled: Boolean = false,
    val printerAddress: String = "",     // Bluetooth MAC or IP
    val printerName: String = "",
    val autoPrint: Boolean = false,
    val maxPrintsPerSession: Int = 2,

    // ── Event ──────────────────────────────────────────────────
    val eventName: String = "האירוע שלנו",
    val galleryUrl: String = "",

    // ── Security ───────────────────────────────────────────────
    val settingsPin: String = "1234",
)

enum class AppTheme(val displayNameHe: String, val displayNameEn: String) {
    DARK_GOLD("אלגנטי / שחור+זהב", "Elegant / Dark Gold"),
    NEON_PARTY("מסיבה / ניאון", "Party / Neon"),
    MINIMAL_WHITE("מינימלי / לבן", "Minimal / White"),
    VINTAGE_RETRO("וינטג' / רטרו", "Vintage / Retro"),
}

enum class AppLanguage(val displayName: String, val isRtl: Boolean) {
    HEBREW("עברית", true),
    ENGLISH("English", false),
    BILINGUAL("עברית + English", true),
}

enum class PhotoFrame(val displayNameHe: String, val displayNameEn: String, val assetName: String) {
    NONE("ללא מסגרת", "No Frame", ""),
    CLASSIC_WHITE("מסגרת לבנה", "Classic White", "frame_classic_white"),
    GOLD_ORNATE("מסגרת זהב", "Gold Ornate", "frame_gold_ornate"),
    FLOWERS("מסגרת פרחים", "Flowers", "frame_flowers"),
    HEARTS("לבבות", "Hearts", "frame_hearts"),
    STARS("כוכבים", "Stars", "frame_stars"),
    NEON_GLOW("זוהר ניאון", "Neon Glow", "frame_neon_glow"),
    FILM_STRIP("סרט פילם", "Film Strip", "frame_film_strip"),
}
