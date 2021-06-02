package org.wltea.analyzer.db.core;

/**
 * @author fsren
 * @date 2021-05-26
 */
public interface Record {

    /**
     * 获取当前行
     *
     * @return 当前行
     */
    Row getCurrentRow();

    /**
     * 移动到下一行
     *
     * @return 若当前已到达最后一行，则返回false，否则返回true
     */
    boolean next();
}
