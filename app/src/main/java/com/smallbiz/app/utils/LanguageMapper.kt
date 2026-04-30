package com.smallbiz.app.utils

/**
 * Maps ISO 4217 currency codes to the local languages spoken in that country.
 *
 * Used for:
 *  1. Speech recognition locale — so the mic understands the local language
 *  2. Keyboard IME hint — so the keyboard suggests the right script/language
 *  3. UI language hint label shown next to the product name field
 *
 * BCP-47 language tags are used (e.g. "ha" = Hausa, "yo" = Yoruba, "ig" = Igbo)
 */
object LanguageMapper {

    /**
     * Returns a list of BCP-47 language tags for a given currency code.
     * The first entry is the primary/preferred language for speech recognition.
     * Multiple entries are shown as keyboard options.
     */
    fun getLanguagesForCurrency(currencyCode: String): List<LanguageOption> {
        return CURRENCY_LANGUAGES[currencyCode.uppercase()]
            ?: listOf(LanguageOption("en", "English", "🇬🇧"))
    }

    fun getPrimaryLanguage(currencyCode: String): LanguageOption =
        getLanguagesForCurrency(currencyCode).first()

    data class LanguageOption(
        val bcp47Tag: String,   // e.g. "ha", "yo", "ig", "en-NG"
        val displayName: String, // e.g. "Hausa"
        val flag: String = ""    // optional emoji
    )

    // ── Currency → Languages map ──────────────────────────────────────────────
    private val CURRENCY_LANGUAGES: Map<String, List<LanguageOption>> = mapOf(

        // ── Nigeria (NGN) — 4 major languages + English ──────────────────────
        "NGN" to listOf(
            LanguageOption("en-NG", "English (Nigeria)", "🇳🇬"),
            LanguageOption("ha",    "Hausa",             "🇳🇬"),
            LanguageOption("yo",    "Yoruba",            "🇳🇬"),
            LanguageOption("ig",    "Igbo",              "🇳🇬"),
            LanguageOption("bin",   "Edo (Bini)",        "🇳🇬")
        ),

        // ── Ghana (GHS) ───────────────────────────────────────────────────────
        "GHS" to listOf(
            LanguageOption("en-GH", "English (Ghana)",  "🇬🇭"),
            LanguageOption("ak",    "Akan (Twi)",       "🇬🇭"),
            LanguageOption("ee",    "Ewe",              "🇬🇭"),
            LanguageOption("dag",   "Dagbani",          "🇬🇭")
        ),

        // ── Kenya (KES) ───────────────────────────────────────────────────────
        "KES" to listOf(
            LanguageOption("sw-KE", "Swahili (Kenya)",  "🇰🇪"),
            LanguageOption("en-KE", "English (Kenya)",  "🇰🇪"),
            LanguageOption("ki",    "Kikuyu",           "🇰🇪")
        ),

        // ── Tanzania (TZS) ────────────────────────────────────────────────────
        "TZS" to listOf(
            LanguageOption("sw-TZ", "Swahili (Tanzania)", "🇹🇿"),
            LanguageOption("en-TZ", "English (Tanzania)", "🇹🇿")
        ),

        // ── Uganda (UGX) ──────────────────────────────────────────────────────
        "UGX" to listOf(
            LanguageOption("sw-UG", "Swahili (Uganda)", "🇺🇬"),
            LanguageOption("en-UG", "English (Uganda)", "🇺🇬"),
            LanguageOption("lg",    "Luganda",          "🇺🇬")
        ),

        // ── South Africa (ZAR) ────────────────────────────────────────────────
        "ZAR" to listOf(
            LanguageOption("en-ZA", "English (S. Africa)", "🇿🇦"),
            LanguageOption("zu",    "Zulu",                "🇿🇦"),
            LanguageOption("xh",    "Xhosa",               "🇿🇦"),
            LanguageOption("af",    "Afrikaans",            "🇿🇦"),
            LanguageOption("st",    "Sotho",                "🇿🇦")
        ),

        // ── Ethiopia (ETB) ────────────────────────────────────────────────────
        "ETB" to listOf(
            LanguageOption("am",    "Amharic",          "🇪🇹"),
            LanguageOption("om",    "Oromo",            "🇪🇹"),
            LanguageOption("ti",    "Tigrinya",         "🇪🇹"),
            LanguageOption("so",    "Somali",           "🇪🇹")
        ),

        // ── Egypt (EGP) ───────────────────────────────────────────────────────
        "EGP" to listOf(
            LanguageOption("ar-EG", "Arabic (Egypt)",   "🇪🇬")
        ),

        // ── Saudi Arabia (SAR) ────────────────────────────────────────────────
        "SAR" to listOf(
            LanguageOption("ar-SA", "Arabic (Saudi)",   "🇸🇦")
        ),

        // ── UAE (AED) ─────────────────────────────────────────────────────────
        "AED" to listOf(
            LanguageOption("ar-AE", "Arabic (UAE)",     "🇦🇪"),
            LanguageOption("en-AE", "English (UAE)",    "🇦🇪")
        ),

        // ── India (INR) ───────────────────────────────────────────────────────
        "INR" to listOf(
            LanguageOption("hi",    "Hindi",            "🇮🇳"),
            LanguageOption("en-IN", "English (India)",  "🇮🇳"),
            LanguageOption("bn",    "Bengali",          "🇮🇳"),
            LanguageOption("ta",    "Tamil",            "🇮🇳"),
            LanguageOption("te",    "Telugu",           "🇮🇳"),
            LanguageOption("mr",    "Marathi",          "🇮🇳"),
            LanguageOption("gu",    "Gujarati",         "🇮🇳"),
            LanguageOption("kn",    "Kannada",          "🇮🇳"),
            LanguageOption("ml",    "Malayalam",        "🇮🇳"),
            LanguageOption("pa",    "Punjabi",          "🇮🇳")
        ),

        // ── China (CNY) ───────────────────────────────────────────────────────
        "CNY" to listOf(
            LanguageOption("zh-CN", "Chinese (Simplified)", "🇨🇳"),
            LanguageOption("zh-TW", "Chinese (Traditional)", "🇨🇳")
        ),

        // ── Japan (JPY) ───────────────────────────────────────────────────────
        "JPY" to listOf(
            LanguageOption("ja",    "Japanese",         "🇯🇵")
        ),

        // ── Brazil (BRL) ──────────────────────────────────────────────────────
        "BRL" to listOf(
            LanguageOption("pt-BR", "Portuguese (Brazil)", "🇧🇷")
        ),

        // ── France / EU (EUR) ─────────────────────────────────────────────────
        "EUR" to listOf(
            LanguageOption("fr",    "French",           "🇫🇷"),
            LanguageOption("de",    "German",           "🇩🇪"),
            LanguageOption("es",    "Spanish",          "🇪🇸"),
            LanguageOption("it",    "Italian",          "🇮🇹"),
            LanguageOption("pt",    "Portuguese",       "🇵🇹"),
            LanguageOption("nl",    "Dutch",            "🇳🇱"),
            LanguageOption("en",    "English",          "🇬🇧")
        ),

        // ── UK (GBP) ──────────────────────────────────────────────────────────
        "GBP" to listOf(
            LanguageOption("en-GB", "English (UK)",     "🇬🇧")
        ),

        // ── USA (USD) ─────────────────────────────────────────────────────────
        "USD" to listOf(
            LanguageOption("en-US", "English (US)",     "🇺🇸"),
            LanguageOption("es-US", "Spanish (US)",     "🇺🇸")
        ),

        // ── Pakistan (PKR) ────────────────────────────────────────────────────
        "PKR" to listOf(
            LanguageOption("ur",    "Urdu",             "🇵🇰"),
            LanguageOption("en-PK", "English (Pakistan)", "🇵🇰"),
            LanguageOption("pa",    "Punjabi",          "🇵🇰")
        ),

        // ── Indonesia (IDR) ───────────────────────────────────────────────────
        "IDR" to listOf(
            LanguageOption("id",    "Indonesian",       "🇮🇩"),
            LanguageOption("jv",    "Javanese",         "🇮🇩")
        ),

        // ── Russia (RUB) ──────────────────────────────────────────────────────
        "RUB" to listOf(
            LanguageOption("ru",    "Russian",          "🇷🇺")
        ),

        // ── Turkey (TRY) ──────────────────────────────────────────────────────
        "TRY" to listOf(
            LanguageOption("tr",    "Turkish",          "🇹🇷")
        ),

        // ── Mexico (MXN) ──────────────────────────────────────────────────────
        "MXN" to listOf(
            LanguageOption("es-MX", "Spanish (Mexico)", "🇲🇽")
        ),

        // ── Philippines (PHP) ─────────────────────────────────────────────────
        "PHP" to listOf(
            LanguageOption("fil",   "Filipino",         "🇵🇭"),
            LanguageOption("en-PH", "English (Philippines)", "🇵🇭")
        ),

        // ── Vietnam (VND) ─────────────────────────────────────────────────────
        "VND" to listOf(
            LanguageOption("vi",    "Vietnamese",       "🇻🇳")
        ),

        // ── Senegal / West Africa (XOF) ───────────────────────────────────────
        "XOF" to listOf(
            LanguageOption("fr",    "French",           "🌍"),
            LanguageOption("wo",    "Wolof",            "🌍"),
            LanguageOption("bm",    "Bambara",          "🌍")
        ),

        // ── Central Africa (XAF) ──────────────────────────────────────────────
        "XAF" to listOf(
            LanguageOption("fr",    "French",           "🌍"),
            LanguageOption("en",    "English",          "🌍")
        ),

        // ── Rwanda (RWF) ──────────────────────────────────────────────────────
        "RWF" to listOf(
            LanguageOption("rw",    "Kinyarwanda",      "🇷🇼"),
            LanguageOption("fr",    "French",           "🇷🇼"),
            LanguageOption("en-RW", "English (Rwanda)", "🇷🇼")
        ),

        // ── Zambia (ZMW) ──────────────────────────────────────────────────────
        "ZMW" to listOf(
            LanguageOption("en-ZM", "English (Zambia)", "🇿🇲"),
            LanguageOption("bem",   "Bemba",            "🇿🇲"),
            LanguageOption("ny",    "Chichewa",         "🇿🇲")
        ),

        // ── Cameroon (XAF) ────────────────────────────────────────────────────
        // (shared with XAF above)

        // ── Morocco (MAD) ─────────────────────────────────────────────────────
        "MAD" to listOf(
            LanguageOption("ar-MA", "Arabic (Morocco)", "🇲🇦"),
            LanguageOption("fr",    "French",           "🇲🇦"),
            LanguageOption("zgh",   "Tamazight (Berber)", "🇲🇦")
        ),

        // ── Côte d'Ivoire / Ivory Coast uses XOF (already mapped above)

        // ── Angola (AOA) ──────────────────────────────────────────────────────
        "AOA" to listOf(
            LanguageOption("pt-AO", "Portuguese (Angola)", "🇦🇴"),
            LanguageOption("ln",    "Lingala",          "🇦🇴")
        ),

        // ── DRC Congo (CDF) ───────────────────────────────────────────────────
        "CDF" to listOf(
            LanguageOption("fr",    "French",           "🇨🇩"),
            LanguageOption("ln",    "Lingala",          "🇨🇩"),
            LanguageOption("sw",    "Swahili",          "🇨🇩")
        ),

        // ── Mozambique (MZN) ──────────────────────────────────────────────────
        "MZN" to listOf(
            LanguageOption("pt-MZ", "Portuguese (Mozambique)", "🇲🇿")
        ),

        // ── Zimbabwe (ZWL) ────────────────────────────────────────────────────
        "ZWL" to listOf(
            LanguageOption("en-ZW", "English (Zimbabwe)", "🇿🇼"),
            LanguageOption("sn",    "Shona",            "🇿🇼"),
            LanguageOption("nd",    "Ndebele",          "🇿🇼")
        ),

        // ── Malawi (MWK) ──────────────────────────────────────────────────────
        "MWK" to listOf(
            LanguageOption("ny",    "Chichewa",         "🇲🇼"),
            LanguageOption("en-MW", "English (Malawi)", "🇲🇼")
        ),

        // ── Sierra Leone (SLL) ────────────────────────────────────────────────
        "SLL" to listOf(
            LanguageOption("en-SL", "English (Sierra Leone)", "🇸🇱"),
            LanguageOption("kri",   "Krio",             "🇸🇱")
        ),

        // ── Liberia (LRD) ─────────────────────────────────────────────────────
        "LRD" to listOf(
            LanguageOption("en-LR", "English (Liberia)", "🇱🇷")
        ),

        // ── Gambia (GMD) ──────────────────────────────────────────────────────
        "GMD" to listOf(
            LanguageOption("en-GM", "English (Gambia)", "🇬🇲"),
            LanguageOption("wo",    "Wolof",            "🇬🇲"),
            LanguageOption("man",   "Mandinka",         "🇬🇲")
        )
    )
}
