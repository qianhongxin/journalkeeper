package com.jd.journalkeeper.core.api;

/**
 * @author liyue25
 * Date: 2019-05-08
 */
public interface RaftEntryHeader {

    short getPartition();
    int getLength();
}