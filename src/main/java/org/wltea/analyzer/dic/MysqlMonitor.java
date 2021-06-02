package org.wltea.analyzer.dic;

/**
 * @author fsren
 * @date 2021-05-25
 */
public class MysqlMonitor implements Runnable{

    @Override
    public void run() {
        Dictionary.getSingleton().reLoadMysqlMainDict();
    }
}
