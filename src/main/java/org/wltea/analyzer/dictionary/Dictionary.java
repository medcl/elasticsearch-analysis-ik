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

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.help.DictionaryHelper;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 词典管理类
 *
 * @author Qicz
 * @since 2021/7/12 23:34
 */
public class Dictionary {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(Dictionary.class.getName());

	private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

	private DictSegment mainDictionary;
	private DictSegment stopWordsDictionary;
	private Configuration configuration;

	private final DefaultDictionary defaultDictionary;

	private final String domain;

	public static Dictionary initial(Configuration configuration, DefaultDictionary defaultDictionary, String domain) {
		return new Dictionary(configuration, defaultDictionary, domain);
	}

	private Dictionary(Configuration configuration, DefaultDictionary defaultDictionary, String domain) {
		this.configuration = configuration;
		this.defaultDictionary = defaultDictionary;
		this.domain = domain;
		this.initial(configuration);
	}

	private void initial(Configuration configuration) {
		this.loadMainDict();
		this.loadStopWordDict();

		if (configuration.isEnableRemoteDict()) {
			logger.info("Remote Dictionary enabled!");
			ConfigurationProperties properties = Configuration.getProperties();
			ConfigurationProperties.Remote.Refresh remoteRefresh = properties.getRemoteRefresh();
			// 建立监控线程 - 主词库
			pool.scheduleAtFixedRate(
					new Monitor(this, DictionaryType.MAIN_WORDS, this.domain),
					remoteRefresh.getDelay(),
					remoteRefresh.getPeriod(),
					TimeUnit.SECONDS);
			// 建立监控线程 - stop词库
			pool.scheduleAtFixedRate(
					new Monitor(this, DictionaryType.STOP_WORDS, this.domain),
					remoteRefresh.getDelay(),
					remoteRefresh.getPeriod(),
					TimeUnit.SECONDS);
		}
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray) {
		Hit hit = this.defaultDictionary.matchInMainDict(charArray);
		if (hit.isMatch()) {
			return hit;
		}
		return this.mainDictionary.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length) {
		Hit hit = this.defaultDictionary.matchInMainDict(charArray, begin, length);
		if (hit.isMatch()) {
			return hit;
		}
		return this.mainDictionary.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
		return this.defaultDictionary.matchInQuantifierDict(charArray, begin, length);
	}

	/**
	 * 判断是否是停止词
	 *
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length) {
		boolean stopWord = this.defaultDictionary.isStopWord(charArray, begin, length);
		if (!stopWord) {
			return true;
		}
		return this.stopWordsDictionary.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict() {
		// 建立一个主词典实例
		this.mainDictionary = new DictSegment((char) 0);

		// 加载远程自定义词库
		this.loadRemoteExtDict(this.mainDictionary, DictionaryType.MAIN_WORDS);
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict() {
		// 建立主词典实例
		this.stopWordsDictionary = new DictSegment((char) 0);

		// 加载远程停用词典
		this.loadRemoteExtDict(this.stopWordsDictionary, DictionaryType.STOP_WORDS);
	}

	private void loadRemoteExtDict(DictSegment dictSegment,
								   DictionaryType dictionaryType) {
		logger.info("[Remote DictFile Loading] for domain {}", this.domain);
		Set<String> remoteWords = DictionaryHelper.getRemoteWords(this, dictionaryType, this.domain);
		// 如果找不到扩展的字典，则忽略
		if (remoteWords.isEmpty()) {
			logger.error("[Remote DictFile Loading] " + this.domain + " load failed");
			return;
		}
		remoteWords.forEach(word -> {
			// 加载远程词典数据到主内存中
			logger.info("[New Word] {}", word);
			dictSegment.fillSegment(word.toLowerCase().toCharArray());
		});
	}

	public String getDomain() {
		return domain;
	}

	/**
	 * 重新加载词典
	 */
	public synchronized void reload(DictionaryType dictionaryType) {
		logger.info("[Begin to reload] ik {} dictionary.", dictionaryType);
		// 新开一个实例加载词典，减少加载过程对当前词典使用的影响
		Dictionary tmpDict = new Dictionary(configuration, defaultDictionary, domain);
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
		logger.info("Reload ik {} dictionary finished.", dictionaryType);
	}
}
