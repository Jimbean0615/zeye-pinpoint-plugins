package com.navercorp.pinpoint.plugin.rocketmq;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author zhangjb <br/>
 * @date 2020-11-24 10:03 <br/>
 * @email: <a href="mailto:zhangjb@cai-inc.com">zhangjb</a> <br/>
 */
public class HostUtil {

    public static String getLocalHost() {
        String ip = "172.0.0.1";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }
        return ip;
    }
}
