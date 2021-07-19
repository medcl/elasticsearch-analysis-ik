/**
 *
 */
package org.wltea.analyzer.configuration;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.openingo.jdkits.validate.AssertKit;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.dictionary.Dictionary;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;

@Slf4j
public class Configuration {
	
	//是否启用智能分词
	private boolean useSmart;

	//是否启用远程词典加载
	private boolean enableRemoteDict;

	//是否启用小写处理
	private boolean enableLowercase;

	private final static String IKANALYZER_YML = "ikanalyzer.yml";

	private static Boolean initialed = false;
	private static RedipConfigurationProperties properties;
	private static String dictRootPath;
	private Dictionary dictionary;

	@Inject
	public Configuration(Environment env, Settings settings) {
		// 从 settings 中获取部分配置
		this.useSmart = "true".equals(settings.get("use_smart", "false"));
		this.enableLowercase = "true".equals(settings.get("enable_lowercase", "true"));
		this.enableRemoteDict = "true".equals(settings.get("enable_remote_dict", "true"));
		// 词源
		String etymology = settings.get("etymology", RemoteDictionaryEtymology.DEFAULT.getEtymology());
		RemoteDictionaryEtymology settingEtymology = RemoteDictionaryEtymology.newEtymology(etymology);
		String message = String.format("the etymology '%s' config is invalid, just support 'redis','mysql','http'.", etymology);
		AssertKit.notNull(settingEtymology, message);
		// 领域
		String domain = settings.get("domain", "default-domain");
		log.info("new configuration for domain '{}' etymology '{}'", domain, etymology);
		// 配置初始化
		Configuration.initial(env);
		// 构造词源及领域
		URI domainUri = URI.create(String.format("%s://%s", etymology, domain));
		this.dictionary = Dictionary.initial(this.enableRemoteDict, domainUri);
	}

	private synchronized static void initial(Environment env) {
		if (Configuration.initialed) {
			return;
		}
		// 加载配置文件
		Path pluginPath = env.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
		Configuration.dictRootPath = pluginPath.toAbsolutePath().toString();
		Path configFile = pluginPath.resolve(IKANALYZER_YML);
		InputStream input = null;
		try {
			log.info("try load config from {}", configFile);
			input = new FileInputStream(configFile.toFile());
		} catch (FileNotFoundException e) {
			pluginPath = getConfigInPluginDir();
			configFile = pluginPath.resolve(IKANALYZER_YML);
			try {
				log.info("try load config from {}", configFile);
				input = new FileInputStream(configFile.toFile());
			} catch (FileNotFoundException ex) {
				// We should report origin exception
				log.error("ik-analyzer", ex);
			}
		}
		if (input != null) {
			InputStream finalInput = input;
			SpecialPermission.check();
			Configuration.properties = AccessController.doPrivileged((PrivilegedAction<RedipConfigurationProperties>) () -> new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader())).loadAs(finalInput, RedipConfigurationProperties.class));
		}

		// 远程词典初始化准备
		RemoteDictionary.initial(Configuration.properties);
		Configuration.initialed = true;
	}

	public static Path getConfigInPluginDir() {
		return PathUtils
				.get(new File(AnalysisIkPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath())
						.getParent(), "config")
				.toAbsolutePath();
	}

	public boolean isUseSmart() {
		return useSmart;
	}

	public Configuration setUseSmart(boolean useSmart) {
		this.useSmart = useSmart;
		return this;
	}

	public boolean isEnableRemoteDict() {
		return enableRemoteDict;
	}

	public boolean isEnableLowercase() {
		return enableLowercase;
	}

	public Dictionary getDictionary() {
		return dictionary;
	}

	public static RedipConfigurationProperties getProperties() {
		return properties;
	}

	public static Path getBaseOnDictRoot(String name) {
		return Configuration.getPath(dictRootPath, name);
	}

	public static String getDictRootPath() {
		return dictRootPath;
	}

	public static Path getPath(String first, String... more) {
		return PathUtils.get(first, more);
	}
}
