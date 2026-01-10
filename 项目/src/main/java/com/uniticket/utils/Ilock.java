package com.uniticket.utils;

public interface Ilock {
    /**
     *
     * @param timeoutSec
     * @return true for success and false for failure
     */
    boolean tryLock(long timeoutSec);
    void unlock();
}
