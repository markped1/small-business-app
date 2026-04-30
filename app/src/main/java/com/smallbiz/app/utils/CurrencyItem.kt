package com.smallbiz.app.utils

import java.util.Currency

data class CurrencyItem(
    val code: String,       // ISO 4217 e.g. "NGN"
    val name: String,       // e.g. "Nigerian Naira"
    val symbol: String,     // e.g. "₦"
    val country: String     // e.g. "Nigeria"
) {
    val displayLabel: String get() = "$flag  $code – $name ($symbol)"
    val searchText: String get() = "$code $name $country $symbol".lowercase()

    // Simple flag emoji from country code (works for most currencies)
    private val flag: String get() = try {
        val cc = countryCode
        if (cc.length == 2) {
            val first = Character.codePointAt(cc, 0) - 'A'.code + 0x1F1E6
            val second = Character.codePointAt(cc, 1) - 'A'.code + 0x1F1E6
            String(Character.toChars(first)) + String(Character.toChars(second))
        } else "🌐"
    } catch (e: Exception) { "🌐" }

    private val countryCode: String get() = CURRENCY_TO_COUNTRY[code] ?: ""

    companion object {
        // Map ISO 4217 → ISO 3166-1 alpha-2 country code for flag emoji
        val CURRENCY_TO_COUNTRY = mapOf(
            "AED" to "AE", "AFN" to "AF", "ALL" to "AL", "AMD" to "AM", "ANG" to "CW",
            "AOA" to "AO", "ARS" to "AR", "AUD" to "AU", "AWG" to "AW", "AZN" to "AZ",
            "BAM" to "BA", "BBD" to "BB", "BDT" to "BD", "BGN" to "BG", "BHD" to "BH",
            "BIF" to "BI", "BMD" to "BM", "BND" to "BN", "BOB" to "BO", "BRL" to "BR",
            "BSD" to "BS", "BTN" to "BT", "BWP" to "BW", "BYN" to "BY", "BZD" to "BZ",
            "CAD" to "CA", "CDF" to "CD", "CHF" to "CH", "CLP" to "CL", "CNY" to "CN",
            "COP" to "CO", "CRC" to "CR", "CUP" to "CU", "CVE" to "CV", "CZK" to "CZ",
            "DJF" to "DJ", "DKK" to "DK", "DOP" to "DO", "DZD" to "DZ", "EGP" to "EG",
            "ERN" to "ER", "ETB" to "ET", "EUR" to "EU", "FJD" to "FJ", "FKP" to "FK",
            "GBP" to "GB", "GEL" to "GE", "GHS" to "GH", "GIP" to "GI", "GMD" to "GM",
            "GNF" to "GN", "GTQ" to "GT", "GYD" to "GY", "HKD" to "HK", "HNL" to "HN",
            "HRK" to "HR", "HTG" to "HT", "HUF" to "HU", "IDR" to "ID", "ILS" to "IL",
            "INR" to "IN", "IQD" to "IQ", "IRR" to "IR", "ISK" to "IS", "JMD" to "JM",
            "JOD" to "JO", "JPY" to "JP", "KES" to "KE", "KGS" to "KG", "KHR" to "KH",
            "KMF" to "KM", "KPW" to "KP", "KRW" to "KR", "KWD" to "KW", "KYD" to "KY",
            "KZT" to "KZ", "LAK" to "LA", "LBP" to "LB", "LKR" to "LK", "LRD" to "LR",
            "LSL" to "LS", "LYD" to "LY", "MAD" to "MA", "MDL" to "MD", "MGA" to "MG",
            "MKD" to "MK", "MMK" to "MM", "MNT" to "MN", "MOP" to "MO", "MRU" to "MR",
            "MUR" to "MU", "MVR" to "MV", "MWK" to "MW", "MXN" to "MX", "MYR" to "MY",
            "MZN" to "MZ", "NAD" to "NA", "NGN" to "NG", "NIO" to "NI", "NOK" to "NO",
            "NPR" to "NP", "NZD" to "NZ", "OMR" to "OM", "PAB" to "PA", "PEN" to "PE",
            "PGK" to "PG", "PHP" to "PH", "PKR" to "PK", "PLN" to "PL", "PYG" to "PY",
            "QAR" to "QA", "RON" to "RO", "RSD" to "RS", "RUB" to "RU", "RWF" to "RW",
            "SAR" to "SA", "SBD" to "SB", "SCR" to "SC", "SDG" to "SD", "SEK" to "SE",
            "SGD" to "SG", "SHP" to "SH", "SLL" to "SL", "SOS" to "SO", "SRD" to "SR",
            "SSP" to "SS", "STN" to "ST", "SVC" to "SV", "SYP" to "SY", "SZL" to "SZ",
            "THB" to "TH", "TJS" to "TJ", "TMT" to "TM", "TND" to "TN", "TOP" to "TO",
            "TRY" to "TR", "TTD" to "TT", "TWD" to "TW", "TZS" to "TZ", "UAH" to "UA",
            "UGX" to "UG", "USD" to "US", "UYU" to "UY", "UZS" to "UZ", "VES" to "VE",
            "VND" to "VN", "VUV" to "VU", "WST" to "WS", "XAF" to "CM", "XCD" to "AG",
            "XOF" to "SN", "XPF" to "PF", "YER" to "YE", "ZAR" to "ZA", "ZMW" to "ZM",
            "ZWL" to "ZW"
        )

        /** Build the full list from Java's Currency class + our country map. */
        fun buildList(): List<CurrencyItem> {
            return Currency.getAvailableCurrencies()
                .mapNotNull { currency ->
                    try {
                        val code = currency.currencyCode
                        val symbol = currency.symbol
                        val name = currency.displayName
                        val country = CURRENCY_TO_COUNTRY[code] ?: ""
                        CurrencyItem(code, name, symbol, country)
                    } catch (e: Exception) { null }
                }
                .sortedWith(compareBy({ it.code != "NGN" }, { it.name })) // NGN first
        }
    }
}
