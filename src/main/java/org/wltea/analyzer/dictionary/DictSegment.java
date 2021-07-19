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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 词典树分段，表示词典树的一个分枝
 */
@Slf4j
class DictSegment implements Comparable<DictSegment> {
	
	// 公用字典表，存储汉字
	private static final Map<Character, Character> CHAR_MAP = new ConcurrentHashMap<Character, Character>(16, 0.95f);
	// 数组大小上限
	private static final int ARRAY_LENGTH_LIMIT = 3;

	// Map存储结构
	private Map<Character, DictSegment> childrenMap;
	// 数组方式存储结构
	private DictSegment[] childrenArray;

	// 当前节点上存储的字符
	private Character nodeChar;

	// 当前节点存储的Segment数目
	// storeSize <=ARRAY_LENGTH_LIMIT ，使用数组存储， storeSize >ARRAY_LENGTH_LIMIT ,则使用Map存储
	private int storeSize = 0;
	// 当前DictSegment状态 ,默认 false , true表示从根节点到当前节点的路径表示一个词
	private boolean nodeState = false;

	DictSegment(Character nodeChar) {
		if (nodeChar == null) {
			throw new IllegalArgumentException("node char cannot be empty");
		}
		this.nodeChar = nodeChar;
	}

	Character getNodeChar() {
		return nodeChar;
	}

	/*
	 * 判断是否有下一个节点
	 */
	boolean hasNextNode() {
		return this.storeSize > 0;
	}

	/**
	 * 匹配词段
	 * @param charArray
	 * @return Hit
	 */
	Hit match(char[] charArray) {
		return this.match(charArray, 0, charArray.length, null);
	}

	/**
	 * 匹配词段
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit
	 */
	Hit match(char[] charArray, int begin, int length) {
		return this.match(charArray, begin, length, null);
	}

	/**
	 * 匹配词段
	 * @param charArray
	 * @param begin
	 * @param length
	 * @param searchHit
	 * @return Hit
	 */
	Hit match(char[] charArray, int begin, int length, Hit searchHit) {
		if (searchHit == null) {
			//如果hit为空，新建
			searchHit = new Hit();
			//设置hit的其实文本位置
			searchHit.setBegin(begin);
		} else {
			//否则要将HIT状态重置
			searchHit.setUnMatch();
		}
		//设置hit的当前处理位置
		searchHit.setEnd(begin);

		Character keyChar = charArray[begin];
		DictSegment ds = null;

		//引用实例变量为本地变量，避免查询时遇到更新的同步问题
		DictSegment[] segmentArray = this.childrenArray;
		Map<Character, DictSegment> segmentMap = this.childrenMap;

		//STEP1 在节点中查找keyChar对应的DictSegment
		if (segmentArray != null) {
			//在数组中查找
			DictSegment keySegment = new DictSegment(keyChar);
			int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
			if (position >= 0) {
				ds = segmentArray[position];
			}

		} else if (segmentMap != null) {
			//在map中查找
			ds = segmentMap.get(keyChar);
		}

		//STEP2 找到DictSegment，判断词的匹配状态，是否继续递归，还是返回结果
		if (ds != null) {
			if (length > 1) {
				//词未匹配完，继续往下搜索
				return ds.match(charArray, begin + 1, length - 1, searchHit);
			} else if (length == 1) {
				//搜索最后一个char
				if (ds.nodeState) {
					//添加HIT状态为完全匹配
					searchHit.setMatch();
				}
				if (ds.hasNextNode()) {
					//添加HIT状态为前缀匹配
					searchHit.setPrefix();
					//记录当前位置的DictSegment
					searchHit.setMatchedDictSegment(ds);
				}
				return searchHit;
			}

		}
		//STEP3 没有找到DictSegment， 将HIT设置为不匹配
		return searchHit;
	}

	/**
	 * 加载填充词典片段
	 * @param charArray
	 * @param enabled true表明一个完整的词，false表示从词典中屏蔽当前词
	 */
	void fillSegment(char[] charArray, boolean enabled) {
		this.fillSegment(charArray, 0, charArray.length, enabled);
	}

	/**
	 * 加载填充词典片段
	 * @param charArray
	 */
	void fillSegment(char[] charArray) {
		this.fillSegment(charArray, 0, charArray.length, true);
	}

	/**
	 * 从文件中加载填充词典片段
	 */
	void fillSegment(Path file, String fileName) {
		try (InputStream is = new FileInputStream(file.toFile())) {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8), 512);
			String word = br.readLine();
			if (word != null) {
				if (word.startsWith("\uFEFF")) {
					word = word.substring(1);
				}
				for (; word != null; word = br.readLine()) {
					word = word.trim();
					if (word.isEmpty()) {
						continue;
					}
					this.fillSegment(word.toCharArray());
				}
			}
		} catch (FileNotFoundException e) {
			log.error("ik-analyzer: " + fileName + " not found", e);
			throw new RuntimeException("ik-analyzer: " + fileName + " not found!!!", e);
		} catch (IOException e) {
			log.error("ik-analyzer: " + fileName + " loading failed", e);
		}
	}

	/**
	 * 屏蔽词典中的一个词
	 * @param charArray
	 */
	void disableSegment(char[] charArray) {
		this.fillSegment(charArray, 0, charArray.length, false);
	}

	/**
	 * 加载填充词典片段
	 * @param charArray
	 * @param begin
	 * @param length
	 * @param enabled true表明一个完整的词，false表示从词典中屏蔽当前词
	 */
	private synchronized void fillSegment(char[] charArray,
										  int begin,
										  int length,
										  boolean enabled) {
		//获取字典表中的汉字对象
		Character beginChar = charArray[begin];
		Character keyChar = CHAR_MAP.get(beginChar);
		// 字典中没有该字，则将其添加入字典
		if (keyChar == null) {
			CHAR_MAP.put(beginChar, beginChar);
			keyChar = beginChar;
		}

		//搜索当前节点的存储，查询对应keyChar的keyChar，如果没有则创建
		DictSegment ds = lookForSegment(keyChar, enabled);
		if (ds != null) {
			// 处理keyChar对应的segment
			if (length > 1) {
				// 词元还没有完全加入词典树
				ds.fillSegment(charArray, begin + 1, length - 1, enabled);
			} else if (length == 1) {
				// 已经是词元的最后一个char,设置当前节点状态为enabled，
				// enabled=true表明一个完整的词，enabled=false表示从词典中屏蔽当前词
				ds.nodeState = enabled;
			}
		}
	}

	/**
	 * 查找本节点下对应的keyChar的segment	 * 
	 * @param keyChar
	 * @param create  =true如果没有找到，则创建新的segment ; =false如果没有找到，不创建，返回null
	 * @return dictSegment
	 */
	private DictSegment lookForSegment(Character keyChar, boolean create) {

		DictSegment ds = null;

		if (this.storeSize <= ARRAY_LENGTH_LIMIT) {
			//获取数组容器，如果数组未创建则创建数组
			DictSegment[] segmentArray = getChildrenArray();
			//搜寻数组
			DictSegment keySegment = new DictSegment(keyChar);
			int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
			if (position >= 0) {
				ds = segmentArray[position];
			}

			//遍历数组后没有找到对应的segment
			if (ds == null && create) {
				ds = keySegment;
				if (this.storeSize < ARRAY_LENGTH_LIMIT) {
					//数组容量未满，使用数组存储
					segmentArray[this.storeSize] = ds;
					//segment数目+1
					this.storeSize++;
					Arrays.sort(segmentArray, 0, this.storeSize);

				} else {
					//数组容量已满，切换Map存储
					//获取Map容器，如果Map未创建,则创建Map
					Map<Character, DictSegment> segmentMap = getChildrenMap();
					//将数组中的segment迁移到Map中
					migrate(segmentArray, segmentMap);
					//存储新的segment
					segmentMap.put(keyChar, ds);
					//segment数目+1 ，  必须在释放数组前执行storeSize++ ， 确保极端情况下，不会取到空的数组
					this.storeSize++;
					//释放当前的数组引用
					this.childrenArray = null;
				}
			}
		} else {
			//获取Map容器，如果Map未创建,则创建Map
			Map<Character, DictSegment> segmentMap = getChildrenMap();
			//搜索Map
			ds = segmentMap.get(keyChar);
			if (ds == null && create) {
				//构造新的segment
				ds = new DictSegment(keyChar);
				segmentMap.put(keyChar, ds);
				//当前节点存储segment数目+1
				this.storeSize++;
			}
		}

		return ds;
	}


	/**
	 * 获取数组容器
	 * 线程同步方法
	 */
	private DictSegment[] getChildrenArray() {
		synchronized (this) {
			if (this.childrenArray == null) {
				this.childrenArray = new DictSegment[ARRAY_LENGTH_LIMIT];
			}
		}
		return this.childrenArray;
	}

	/**
	 * 获取Map容器
	 * 线程同步方法
	 */
	private Map<Character, DictSegment> getChildrenMap() {
		synchronized (this) {
			if (this.childrenMap == null) {
				this.childrenMap = new ConcurrentHashMap<Character, DictSegment>(ARRAY_LENGTH_LIMIT * 2, 0.8f);
			}
		}
		return this.childrenMap;
	}

	/**
	 * 将数组中的segment迁移到Map中
	 * @param segmentArray
	 */
	private void migrate(DictSegment[] segmentArray, Map<Character, DictSegment> segmentMap) {
		for (DictSegment segment : segmentArray) {
			if (segment != null) {
				segmentMap.put(segment.nodeChar, segment);
			}
		}
	}

	/**
	 * 实现Comparable接口
	 * @param o
	 * @return int
	 */
	public int compareTo(DictSegment o) {
		//对当前节点存储的char进行比较
		return this.nodeChar.compareTo(o.nodeChar);
	}

}
