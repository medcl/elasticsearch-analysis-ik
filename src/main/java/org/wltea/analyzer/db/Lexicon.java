package org.wltea.analyzer.db;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author fsren
 * @date 2021-05-26
 */
public class Lexicon implements Serializable {

    private static final long serialVersionUID = 7628519160135272308L;
    /**
     * 扩展词
     */
    private String lexiconText;

    /**
     * 加载状态  true 代表加载, false 代表屏蔽
     */
    private Boolean isFill;

    /**
     * 最后更新时间
     */
    private Timestamp modifyDate;

    public String getLexiconText() {
        return lexiconText;
    }

    public void setLexiconText(String lexiconText) {
        this.lexiconText = lexiconText;
    }

    public Boolean getFill() {
        return isFill;
    }

    public void setFill(Boolean fill) {
        isFill = fill;
    }

    public Timestamp getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Timestamp modifyDate) {
        this.modifyDate = modifyDate;
    }
}
