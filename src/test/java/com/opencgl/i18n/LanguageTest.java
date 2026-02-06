package com.opencgl.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import com.opencgl.api.PluginI18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LanguageTest {

    @AfterEach
    void resetPlugins() {
        I18N.clearRegisteredPlugins();
    }

    @Test
    void exposesTheLocaleAssignedToEachLanguage() {
        assertEquals(Locale.ENGLISH, Language.ENGLISH.getLocale());
        assertEquals(Locale.SIMPLIFIED_CHINESE, Language.SIMPLIFIED_CHINESE.getLocale());
    }

    @Test
    void clearsPluginLanguageListenersBeforeHotReload() {
        PluginI18n plugin = locale -> { };
        I18N.registerPlugin(plugin);
        assertEquals(1, I18N.registeredPluginCount());

        I18N.clearRegisteredPlugins();

        assertEquals(0, I18N.registeredPluginCount());
    }
}
