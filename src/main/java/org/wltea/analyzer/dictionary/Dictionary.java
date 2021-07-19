/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.dictionary;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.SpecialPermission;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.dictionary.IDictionary;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.help.StringHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 词典管理类
 *
 * @author Qicz
 * @since 2021/7/12 23:34
 */
@Slf4j
public class Dictionary implements IDictionary {
	
	private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

	private DictSegment mainDictionary;
	private DictSegment quantifierDictionary;
	private DictSegment stopWordsDictionary;

	private final URI domainUri;

	private static final String PATH_DIC_MAIN = "main.dic";
	private static final String PATH_DIC_QUANTIFIER = "quantifier.dic";
	private static final String PATH_DIC_STOP = "stopword.dic";
	private final boolean enableRemoteDict;

	private final static Map<String, Dictionary> DOMAIN_DICTIONARY_MAPPING = new ConcurrentHashMap<>();

	public static synchronized Dictionary initial(boolean enableRemoteDict, URI domainUri) {
		String key = domainUri.toString();
		Dictionary dictionary = null;
		if (!DOMAIN_DICTIONARY_MAPPING.containsKey(key)) {
			dictionary = new Dictionary(enableRemoteDict, domainUri);
			DOMAIN_DICTIONARY_MAPPING.put(key, dictionary);
		} else {
			dictionary = DOMAIN_DICTIONARY_MAPPING.get(key);
		}
		return dictionary;
	}

	private Dictionary(boolean enableRemoteDict, URI domainUri) {
		this(enableRemoteDict, enableRemoteDict, domainUri);
	}

	private Dictionary(boolean enableRemoteDict, boolean enableMonitor, URI domainUri) {
		this.enableRemoteDict = enableRemoteDict;
		this.domainUri = domainUri;
		this.initial(enableMonitor);
	}

	private void initial(boolean enableMonitor) {
		this.loadMainDict();
		this.loadQuantifierDict();
		this.loadStopWordDict();

		if (this.enableRemoteDict && enableMonitor) {
			log.info("Remote Dictionary enabled for '{}'!", this.domainUri);
			RedipConfigurationProperties properties = Configuration.getProperties();
			RedipConfigurationProperties.Remote.Refresh remoteRefresh = properties.getRemoteRefresh();
			// 建立监控线程 - 主词库
			pool.scheduleAtFixedRate(
					new Monitor(this, DictionaryType.MAIN_WORDS, this.domainUri),
					remoteRefresh.getDelay(),
					remoteRefresh.getPeriod(),
					TimeUnit.SECONDS);
			// 建立监控线程 - stop词库
			pool.scheduleAtFixedRate(
					new Monitor(this, DictionaryType.STOP_WORDS, this.domainUri),
					remoteRefresh.getDelay(),
					remoteRefresh.getPeriod(),
					TimeUnit.SECONDS);
		}
	}

	/**
	 * 重新加载词典
	 */
	@Override
	public synchronized void reload(DictionaryType dictionaryType) {
		log.info("[Begin to reload] ik '{}' dictionary.", dictionaryType);
		// 新开一个实例加载词典，减少加载过程对当前词典使用的影响
		// 无需多余的monitor
		Dictionary tmpDict = new Dictionary(this.enableRemoteDict, false, domainUri);
		switch (dictionaryType) {
			case MAIN_WORDS: {
				tmpDict.loadMainDict();
				this.mainDictionary = tmpDict.mainDictionary;
			}
			break;
			case STOP_WORDS: {
				tmpDict.loadStopWordDict();
				this.stopWordsDictionary = tmpDict.stopWordsDictionary;
			}
			break;
			default: {
				tmpDict.loadMainDict();
				tmpDict.loadStopWordDict();
				this.mainDictionary = tmpDict.mainDictionary;
				this.stopWordsDictionary = tmpDict.stopWordsDictionary;
			}
		}
		log.info("Reload ik '{}' dictionary finished.", dictionaryType);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray) {
		return this.mainDictionary.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length) {
		log.info("matchInMainDict for '{}'", this.domainUri);
		return this.mainDictionary.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
		log.info("matchInQuantifierDict for '{}'", this.domainUri);
		return this.quantifierDictionary.match(charArray, begin, length);
	}

	/**
	 * 判断是否是停止词
	 *
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length) {
		log.info("isStopWord for '{}'", this.domainUri);
		return this.stopWordsDictionary.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict() {
		// 建立一个主词典实例
		this.mainDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = Configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_MAIN);
		this.mainDictionary.fillSegment(file, "Main DictFile");
		// 加载扩展词典
		List<String> mainExtDictFiles = Configuration.getProperties().getLocalMainExtDictFiles();
		this.loadLocalExtDict(this.mainDictionary, mainExtDictFiles, "Main Extra DictFile");

		// 加载远程自定义词库
		this.loadRemoteExtDict(this.mainDictionary, DictionaryType.MAIN_WORDS);
	}

	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict() {
		// 建立一个量词典实例
		this.quantifierDictionary = new DictSegment((char) 0);
		// 读取量词词典文件
		Path file = Configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_QUANTIFIER);
		this.quantifierDictionary.fillSegment(file,  "Quantifier");
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict() {
		// 建立主词典实例
		this.stopWordsDictionary = new DictSegment((char) 0);
		// 读取主词典文件
		Path file = Configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_STOP);
		this.stopWordsDictionary.fillSegment(file, "Main Stopwords");

		// 加载扩展停止词典
		List<String> extStopDictFiles = Configuration.getProperties().getLocalStopExtDictFiles();
		this.loadLocalExtDict(this.stopWordsDictionary, extStopDictFiles, "Extra Stopwords");

		// 加载远程停用词典
		this.loadRemoteExtDict(this.stopWordsDictionary, DictionaryType.STOP_WORDS);
	}

	private void loadLocalExtDict(DictSegment dictSegment,
								  List<String> extDictFiles,
								  String name) {
		// 加载扩展词典配置
		extDictFiles = this.walkFiles(extDictFiles);
		extDictFiles.forEach(extDictName -> {
			// 读取扩展词典文件
			log.info("[Local DictFile Loading] " + extDictName);
			Path file = Configuration.getPath(extDictName);
			dictSegment.fillSegment(file, name);
		});
	}

	private void loadRemoteExtDict(DictSegment dictSegment,
								   DictionaryType dictionaryType) {
		if (!this.enableRemoteDict) {
			return;
		}
		log.info("[Remote DictFile Loading] for domain '{}'", this.domainUri);
		SpecialPermission.check();
		Set<String> remoteWords = RemoteDictionary.getRemoteWords(dictionaryType, this.domainUri);
		// 如果找不到扩展的字典，则忽略
		if (remoteWords.isEmpty()) {
			log.info("[Remote DictFile Loading] no new words for '{}'", this.domainUri);
			return;
		}
		remoteWords.forEach(word -> {
			// 加载远程词典数据到主内存中
			log.info("[New '{}' Word] '{}'", dictionaryType.getDictName(), word);
			dictSegment.fillSegment(word.toLowerCase().toCharArray());
		});
	}

	private List<String> walkFiles(List<String> files) {
		List<String> extDictFiles = new ArrayList<>(files.size());
		files.forEach(filePath -> {
			Path path = Configuration.getBaseOnDictRoot(filePath);
			if (Files.isRegularFile(path)) {
				extDictFiles.add(path.toString());
			} else if (Files.isDirectory(path)) {
				try {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							extDictFiles.add(file.toString());
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException e) {
							log.error("[Ext Loading] listing files", e);
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					log.error("[Ext Loading] listing files", e);
				}
			} else {
				log.warn("[Ext Loading] file not found: " + path);
			}
		});
		return StringHelper.filterBlank(extDictFiles);
	}
}
