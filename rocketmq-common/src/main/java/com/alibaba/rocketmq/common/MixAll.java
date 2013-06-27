package com.alibaba.rocketmq.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import sun.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;


/**
 * 各种方法大杂烩
 * 
 * @author shijia.wxr<vintage.wang@gmail.com>
 * @author lansheng.zj@taobao.com
 */
public class MixAll {
    public static final String ROCKETMQ_HOME_ENV = "ROCKETMQ_HOME";
    public static final String ROCKETMQ_HOME_PROPERTY = "rocketmq.home.dir";

    public static final String NAMESRV_ADDR_ENV = "NAMESRV_ADDR";
    public static final String NAMESRV_ADDR_PROPERTY = "rocketmq.namesrv.addr";

    public static final String WS_DOMAIN_NAME = "jmenv.tbsite.net";
    // http://jmenv.tbsite.net:8080/rocketmq/nsaddr
    public static final String WS_ADDR = "http://" + WS_DOMAIN_NAME + ":8080/rocketmq/nsaddr";
    public static final String DEFAULT_TOPIC = "TBW102";
    public static final String DEFAULT_PRODUCER_GROUP = "DEFAULT_PRODUCER";
    public static final String DEFAULT_CONSUMER_GROUP = "DEFAULT_CONSUMER";
    public static final String SELF_TEST_TOPIC = "SELF_TEST_TOPIC";
    public static final long TotalPhysicalMemorySize = getTotalPhysicalMemorySize();
    public static final List<String> LocalInetAddrs = getLocalInetAddress();
    public static final String Localhost = localhost();
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final long MASTER_ID = 0L;
    public static final long CURRENT_JVM_PID = getPID();

    // 为每个Consumer Group建立一个默认的Topic，前缀 + GroupName，用来保存处理失败需要重试的消息
    public static final String RETRY_GROUP_TOPIC_PREFIX = "%RETRY%";
    // 为每个Consumer Group建立一个默认的Topic，前缀 + GroupName，用来保存重试多次都失败，接下来不再重试的消息
    public static final String DLQ_GROUP_TOPIC_PREFIX = "%DLQ%";


    public static String getRetryTopic(final String consumerGroup) {
        return RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
    }


    public static String getDLQTopic(final String consumerGroup) {
        return DLQ_GROUP_TOPIC_PREFIX + consumerGroup;
    }


    public static long getPID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null && processName.length() > 0) {
            try {
                return Long.parseLong(processName.split("@")[0]);
            }
            catch (Exception e) {
                return 0;
            }
        }

        return 0;
    }


    public static final String file2String(final String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            char[] data = new char[(int) file.length()];
            boolean result = false;

            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
                int len = fileReader.read(data);
                result = (len == data.length);
            }
            catch (IOException e) {
                // e.printStackTrace();
            }
            finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (result) {
                String value = new String(data);
                return value;
            }
        }
        return null;
    }


    public static long createBrokerId(final String ip, final int port) {
        InetSocketAddress isa = new InetSocketAddress(ip, port);
        byte[] ipArray = isa.getAddress().getAddress();
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(ipArray);
        bb.putInt(port);
        long value = bb.getLong(0);
        return Math.abs(value);
    }


    public static final boolean string2File(final String str, final String fileName) {
        File file = new File(fileName);
        File fileParent = file.getParentFile();
        if (fileParent != null) {
            fileParent.mkdirs();
        }
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(str);
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fileWriter != null)
                try {
                    fileWriter.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return false;
    }


    public static String findClassPath(Class<?> c) {
        URL url = c.getProtectionDomain().getCodeSource().getLocation();
        return url.getPath();
    }


    public static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("h", "help", false, "Print help");
        opt.setRequired(false);
        options.addOption(opt);

        opt =
                new Option("n", "namesrvAddr", true,
                    "Name server address list, eg: 192.168.1.100:9876;192.168.1.101:9876");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }


    public static CommandLine parseCmdLine(final String appName, String[] args, Options options,
            CommandLineParser parser) {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
            if (commandLine.hasOption('h')) {
                hf.printHelp(appName, options, true);
                return null;
            }
        }
        catch (ParseException e) {
            hf.printHelp(appName, options, true);
        }

        return commandLine;
    }


    public static void printCommandLineHelp(final String appName, final Options options) {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        hf.printHelp(appName, options, true);
    }


    public static Properties commandLine2Properties(final CommandLine commandLine) {
        Properties properties = new Properties();
        Option[] opts = commandLine.getOptions();

        if (opts != null) {
            for (Option opt : opts) {
                String name = opt.getLongOpt();
                String value = commandLine.getOptionValue(name);
                if (value != null) {
                    properties.setProperty(name, value);
                }
            }
        }

        return properties;
    }


    public static void printObjectProperties(final Logger log, final Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                        if (null == value) {
                            value = "";
                        }
                    }
                    catch (IllegalArgumentException e) {
                        System.out.println(e);
                    }
                    catch (IllegalAccessException e) {
                        System.out.println(e);
                    }
                    if (log != null) {
                        log.info(name + "=" + value);
                    }
                    else {
                        System.out.println(name + "=" + value);
                    }
                }
            }
        }
    }


    /**
     * 获取机器的物理内存
     * 
     * @return 单位字节
     */
    public static long getTotalPhysicalMemorySize() {
        OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long physicalTotal = osmxb.getTotalPhysicalMemorySize();
        return physicalTotal;
    }


    public static String properties2String(final Properties properties) {
        Set<Object> sets = properties.keySet();
        StringBuilder sb = new StringBuilder();
        for (Object key : sets) {
            Object value = properties.get(key);
            if (value != null) {
                sb.append(key.toString() + "=" + value.toString() + IOUtils.LINE_SEPARATOR);
            }
        }

        return sb.toString();
    }


    /**
     * 字符串转化成Properties 字符串和Properties配置文件格式一样
     */
    public static Properties string2Properties(final String str) {
        Properties properties = new Properties();
        try {
            InputStream in = new ByteArrayInputStream(str.getBytes(DEFAULT_CHARSET));
            properties.load(in);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return properties;
    }


    /**
     * 将对象各成员属性值转化为Properties
     */
    public static Properties object2Properties(final Object object) {
        Properties properties = new Properties();

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                    }
                    catch (IllegalArgumentException e) {
                        System.out.println(e);
                    }
                    catch (IllegalAccessException e) {
                        System.out.println(e);
                    }

                    if (value != null) {
                        properties.setProperty(name, value.toString());
                    }
                }
            }
        }

        return properties;
    }


    /**
     * 将Properties中的值写入Object
     */
    public static void properties2Object(final Properties p, final Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    String tmp = mn.substring(4);
                    String first = mn.substring(3, 4);

                    String key = first.toLowerCase() + tmp;
                    String property = p.getProperty(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg = null;
                            if (cn.equals("int")) {
                                arg = Integer.parseInt(property);
                            }
                            else if (cn.equals("long")) {
                                arg = Long.parseLong(property);
                            }
                            else if (cn.equals("double")) {
                                arg = Double.parseDouble(property);
                            }
                            else if (cn.equals("boolean")) {
                                arg = Boolean.parseBoolean(property);
                            }
                            else if (cn.equals("String")) {
                                arg = property;
                            }
                            else {
                                continue;
                            }
                            method.invoke(object, new Object[] { arg });
                        }
                    }
                }
                catch (Throwable e) {
                }
            }
        }
    }


    public static boolean isPropertiesEqual(final Properties p1, final Properties p2) {
        return p1.equals(p2);
    }


    //
    // public static Logger createLogger(final String loggerName) {
    // Logger logger = LoggerFactory.getLogger(loggerName);
    // LoggerContext loggerContext = (LoggerContext)
    // LoggerFactory.getILoggerFactory();
    //
    // ch.qos.logback.classic.Logger newLogger = (ch.qos.logback.classic.Logger)
    // logger;
    // // Remove all previously added appenders from this logger instance.
    // newLogger.detachAndStopAllAppenders();
    //
    // // define appender
    // RollingFileAppender<ILoggingEvent> appender = new
    // RollingFileAppender<ILoggingEvent>();
    //
    // // policy
    // TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new
    // TimeBasedRollingPolicy<ILoggingEvent>();
    // rollingPolicy.setContext(loggerContext);
    // rollingPolicy.setFileNamePattern(System.getProperty("user.home") +
    // File.separator
    // + "rocketmqlogs/rocketmq_" + loggerName + "-%d{yyyy-MM-dd}.log");
    // rollingPolicy.setParent(appender);
    // rollingPolicy.start();
    //
    // // encoder
    // PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    // encoder.setContext(loggerContext);
    // encoder.setPattern("%d{yyy-MM-dd HH:mm:ss,GMT+8} %p %t - %m%n");
    // encoder.setCharset(Charset.forName("UTF-8"));
    // encoder.start();
    //
    // // start appender
    // appender.setRollingPolicy(rollingPolicy);
    // appender.setContext(loggerContext);
    // appender.setEncoder(encoder);
    // appender.setPrudent(true); // support that multiple JVMs can safely
    // // write to the same file.
    // appender.start();
    //
    // newLogger.addAppender(appender);
    //
    // // setup level
    // newLogger.setLevel(Level.INFO);
    // // remove the appenders that inherited 'ROOT'.
    // newLogger.setAdditive(true);
    // return newLogger;
    // }

    public static List<String> getLocalInetAddress() {
        List<String> inetAddressList = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    inetAddressList.add(addrs.nextElement().getHostAddress());
                }
            }
        }
        catch (SocketException e) {
            throw new RuntimeException("get local inet address fail", e);
        }

        return inetAddressList;
    }


    public static boolean isLocalAddr(String address) {
        for (String addr : LocalInetAddrs) {
            if (address.contains(addr))
                return true;
        }
        return false;
    }


    private static String localhost() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("get localhost fail", e);
        }
    }


    public Set<String> list2Set(List<String> values) {
        Set<String> result = new HashSet<String>();
        for (String v : values) {
            result.add(v);
        }
        return result;
    }


    public List<String> set2List(Set<String> values) {
        List<String> result = new ArrayList<String>();
        for (String v : values) {
            result.add(v);
        }
        return result;
    }


    public static void compareAndIncreaseOnly(final AtomicLong target, final long value) {
        long prev = target.get();
        while (value > prev) {
            boolean updated = target.compareAndSet(prev, value);
            if (updated)
                break;

            prev = target.get();
        }
    }
}
