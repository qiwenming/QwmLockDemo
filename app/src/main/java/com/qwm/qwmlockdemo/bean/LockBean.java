package com.qwm.qwmlockdemo.bean;

import java.io.Serializable;

/**
 * @author qiwenming
 * @date 2016/2/22 0022 下午 2:06
 * @ClassName: LockBean
 * @ProjectName:
 * @PackageName: com.qwm.qwmlockdemo.bean
 * @Description: 锁的一些参数的javaBean
 */
public class LockBean implements Serializable{
    public LockBean(String addressStr, String bauteRateStr, String boardAddStr, String lockAddStr) {
        this.addressStr = addressStr;
        this.bauteRateStr = bauteRateStr;
        this.boardAddStr = boardAddStr;
        this.lockAddStr = lockAddStr;
    }

    public LockBean() {
    }

    /**
     *    硬件地址
     */
    public String addressStr;
    /**
     * 波特率
     */
    public String bauteRateStr;
    /**
     * 板地址
     */
    public String boardAddStr;
    /**
     * 锁地址
     */
    public String lockAddStr;
}
