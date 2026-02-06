

package com.opencgl.i18n;

import java.util.Locale;

/**
 * Enumerator to list all the supported {@link Locale}s by MaterialFX.
 * <p>
 * Every {@code Language} enumeration is associated with a {@code Locale}.
 * <p>
 * The enumerator also specifies the project's default language, {@link #defaultLanguage()}.
 */
public enum Language {
    ENGLISH(Locale.ENGLISH),
    SIMPLIFIED_CHINESE(Locale.SIMPLIFIED_CHINESE);

    private final Locale locale;

    Language(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public static Language defaultLanguage() {
        return SIMPLIFIED_CHINESE;
    }

}
