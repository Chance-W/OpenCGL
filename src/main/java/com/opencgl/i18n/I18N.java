package com.opencgl.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import com.opencgl.base.utils.i18n.BaseI18N;
import com.opencgl.base.utils.i18n.BaseLanguage;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.opencgl.api.PluginI18n;
import com.opencgl.base.utils.i18n.I18nResolver;

/**
 * 主程序国际化处理。
 * 此类现在将 Locale 状态同步至 com.opencgl.base.utils.i18n.I18N。
 */
public class I18N {

	// 实例化主程序专属的解析器
	private static final I18nResolver CORE_RESOLVER = new I18nResolver("com/opencgl/i18n/opencgl");

	// 注册的插件列表（用于语言切换通知）
	private static final List<PluginI18n> plugins = new CopyOnWriteArrayList<>();

	/**
	 * 获取当前指定的 Locale
	 */
	public static Locale getLocale() {
		return BaseI18N.getLocale();
	}

	/**
	 * 获取全局语言状态属性
	 */
	public static ObjectProperty<Locale> localeProperty() {
		return BaseI18N.localeProperty();
	}

	/**
	 * 设置全局语言
	 */
	public static void setLanguage(Language language) {
		BaseLanguage baseLang = BaseLanguage.valueOf(language.name());
		// 先通知插件使用新 locale，再更新全局 locale，这样 sidebar 等依赖 localeProperty 的绑定重算时，
		// 各插件的 directoryName()/name() 已是最新语言
		Locale newLocale = language.getLocale();
		notifyPlugins(newLocale);
		BaseI18N.setLanguage(baseLang);
	}

	/**
	 * 注册需要监听语言切换的插件
	 */
	public static void registerPlugin(PluginI18n plugin) {
		if (plugin != null && !plugins.contains(plugin)) {
			plugins.add(plugin);
			// 立即通知一次当前语言
			plugin.onLanguageChange(getLocale());
		}
	}

	/** Releases old plugin instances before their ClassLoaders are closed. */
	public static void clearRegisteredPlugins() {
		plugins.clear();
	}

	static int registeredPluginCount() {
		return plugins.size();
	}

	private static void notifyPlugins(Locale locale) {
		for (PluginI18n plugin : plugins) {
			try {
				plugin.onLanguageChange(locale);
			} catch (Exception e) {
				// 忽略插件异常，防止影响主程序
			}
		}
	}

	// ====== 静态代理方法，底层使用 CORE_RESOLVER ======

	public static String get(String key, Object... args) {
		return CORE_RESOLVER.get(key, args);
	}

	public static String getOrDefault(String key, Object... args) {
		return CORE_RESOLVER.getOrDefault(key, args);
	}

	public static String getOrDefault(String key, String def, Object... args) {
		return CORE_RESOLVER.getOrDefault(key, def, args);
	}

	public static StringBinding getBinding(String key, Object... args) {
		return CORE_RESOLVER.getBinding(key, args);
	}

	public static StringBinding getBinding(Callable<String> callable) {
		return CORE_RESOLVER.getBinding(callable);
	}

	public static ResourceBundle getBundle(Locale locale) {
		return CORE_RESOLVER.getBundle(locale);
	}

	public static Language[] getSupportedLanguages() {
		return Language.values();
	}
}
