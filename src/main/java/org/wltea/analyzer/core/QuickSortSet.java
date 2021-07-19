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
package org.wltea.analyzer.core;

import java.util.Objects;

/**
 * IK分词器专用的Lexeme快速排序集合
 */
class QuickSortSet {
	//链表头
	private Cell head;
	//链表尾
	private Cell tail;
	//链表的实际大小
	private int size;

	QuickSortSet() {
		this.size = 0;
	}

	/**
	 * 向链表集合添加词元
	 */
	boolean addLexeme(Lexeme lexeme) {
		Cell newCell = new Cell(lexeme);
		if (this.size == 0) {
			this.head = newCell;
			this.tail = newCell;
			this.size++;
			return true;
		}
		// 词元与尾部词元相同，不放入集合
		if (this.tail.compareTo(newCell) == 0) {
			return false;
		}
		// 词元接入链表尾部
		if (this.tail.compareTo(newCell) < 0) {
			this.tail.next = newCell;
			newCell.prev = this.tail;
			this.tail = newCell;
			this.size++;
			return true;
		}

		// 词元接入链表头部
		if (this.head.compareTo(newCell) > 0) {
			this.head.prev = newCell;
			newCell.next = this.head;
			this.head = newCell;
			this.size++;
			return true;

		}
		// 从尾部上逆
		Cell index = this.tail;
		while (index != null && index.compareTo(newCell) > 0) {
			index = index.prev;
		}

		// 词元与集合中的词元重复，不放入集合
		if (Objects.isNull(index)
				|| index.compareTo(newCell) == 0) {
			return false;
		}
		// 词元插入链表中的某个位置
		if (index.compareTo(newCell) < 0) {
			newCell.prev = index;
			newCell.next = index.next;
			index.next.prev = newCell;
			index.next = newCell;
			this.size++;
			return true;
		}
		return false;
	}

	/**
	 * 返回链表头部元素
	 */
	Lexeme peekFirst() {
		if (this.head != null) {
			return this.head.lexeme;
		}
		return null;
	}

	/**
	 * 取出链表集合的第一个元素
	 * @return Lexeme
	 */
	Lexeme pollFirst() {
		if (this.size == 1) {
			Lexeme first = this.head.lexeme;
			this.head = null;
			this.tail = null;
			this.size--;
			return first;
		}

		if (this.size > 1) {
			Lexeme first = this.head.lexeme;
			this.head = this.head.next;
			this.size--;
			return first;
		}
		return null;
	}

	/**
	 * 返回链表尾部元素
	 */
	Lexeme peekLast() {
		if (this.tail != null) {
			return this.tail.lexeme;
		}
		return null;
	}

	/**
	 * 取出链表集合的最后一个元素
	 * @return Lexeme
	 */
	Lexeme pollLast() {
		if (this.size == 1) {
			Lexeme last = this.head.lexeme;
			this.head = null;
			this.tail = null;
			this.size--;
			return last;
		}

		if (this.size > 1) {
			Lexeme last = this.tail.lexeme;
			this.tail = this.tail.prev;
			this.size--;
			return last;
		}
		return null;
	}

	/**
	 * 返回集合大小
	 */
	int size() {
		return this.size;
	}

	/**
	 * 判断集合是否为空
	 */
	boolean isEmpty() {
		return this.size == 0;
	}

	/**
	 * 返回lexeme链的头部
	 */
	Cell getHead() {
		return this.head;
	}

	/**
	 *
	 * IK 中文分词  版本 5.0
	 * IK Analyzer release 5.0
	 *
	 * Licensed to the Apache Software Foundation (ASF) under one or more
	 * contributor license agreements.  See the NOTICE file distributed with
	 * this work for additional information regarding copyright ownership.
	 * The ASF licenses this file to You under the Apache License, Version 2.0
	 * (the "License"); you may not use this file except in compliance with
	 * the License.  You may obtain a copy of the License at
	 *
	 *     http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 *
	 * 源代码由林良益(linliangyi2005@gmail.com)提供
	 * 版权声明 2012，乌龙茶工作室
	 * provided by Linliangyi and copyright 2012 by Oolong studio
	 *
	 * QuickSortSet集合单元
	 *
	 */
	static class Cell implements Comparable<Cell> {
		private Cell prev;
		private Cell next;
		private Lexeme lexeme;

		Cell(Lexeme lexeme) {
			if (lexeme == null) {
				throw new IllegalArgumentException("lexeme must not be null");
			}
			this.lexeme = lexeme;
		}

		@Override
		public int compareTo(Cell o) {
			return this.lexeme.compareTo(o.lexeme);
		}

		public Cell getPrev() {
			return this.prev;
		}

		public Cell getNext() {
			return this.next;
		}

		public Lexeme getLexeme() {
			return this.lexeme;
		}
	}
}
