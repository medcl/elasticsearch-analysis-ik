package org.elasticsearch;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.junit.Test;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * TestSettings
 *
 * @author Qicz
 * @since 2021/7/9 10:02
 */
public class TestIk {

	@Test
	public void readYml() {
		try {
		URL resource = Thread.currentThread().getContextClassLoader().getResource("ikanalyzer.yml");
		File file = new File(resource.toURI());
		Path path = file.toPath();
		final Settings customSettings;

			customSettings = Settings.builder().loadFromPath(path).build();
			assert customSettings != null;
			System.out.println(customSettings);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to load settings", e);
		}
	}

	@Test
	public void loadYml() {
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(RedipConfigurationProperties.class, TestIk.class.getClassLoader()));
		InputStream resourceAsStream = TestIk.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
		RedipConfigurationProperties map = yaml.loadAs(resourceAsStream, RedipConfigurationProperties.class);
		System.out.println(map);
	}

	@Test
	public void analyze() throws Exception {
		String path = System.getProperty("user.dir");
		Environment environment = new Environment(Settings.builder().put("path.home", path).build(), null);
		Settings settings = Settings.builder()
				.put("use_smart", false)
				.put("enable_lowercase", false)
				.put("enable_remote_dict", true)
				.put("domain", "ik-domain")
				.build();
		final Configuration configuration = new Configuration(environment, settings).setUseSmart(false);


		TimeUnit.SECONDS.sleep(3);

		String text = "新词来了";

		RemoteDictionary.addWord(RemoteDictionaryEtymology.REDIS,
				DictionaryType.MAIN_WORDS, "ik-domain", text, "有一个新词来了");


		RemoteDictionary.addWord(RemoteDictionaryEtymology.REDIS,
				DictionaryType.STOP_WORDS, "ik-domain", "新词stop来了", "有一个新词stop来了");
		this.analyze(configuration, text);
		System.in.read();
	}

	private void analyze(Configuration configuration, String text) throws IOException {
		IKSegmenter segmenter = new IKSegmenter(new StringReader(text), configuration);
		Lexeme next;
		System.out.print("非智能分词结果(ik_max_world)：");
		StringJoiner stringJoiner = new StringJoiner(",");
		while((next=segmenter.next())!=null){
			String lexemeText = next.getLexemeText();
			stringJoiner.add(lexemeText);
		}
		System.out.println(stringJoiner);
		System.out.println();
		System.out.println("----------------------------分割线------------------------------");

		configuration.setUseSmart(true); // 切换配置
		IKSegmenter smartSegmenter = new IKSegmenter(new StringReader(text), configuration);
		System.out.print("智能分词结果(ik_smart)：");
		stringJoiner = new StringJoiner(",");
		while((next=smartSegmenter.next())!=null) {
			String lexemeText = next.getLexemeText();
			stringJoiner.add(lexemeText);
		}
		System.out.println(stringJoiner);
	}
}
