# pinpoint-plugins

基于pinpoint2.0定制pinpoint插件，计划完成多线程插件，rocketmq插件，mybatis插件等。


# Implementing a Profiler Plugin
A Pinpoint profiler plugin have to provide implementations of [ProfilerPlugin](https://github.com/naver/pinpoint/blob/master/bootstrap-core/src/main/java/com/navercorp/pinpoint/bootstrap/plugin/ProfilerPlugin.java) and [TraceMetadataProvider](https://github.com/naver/pinpoint/blob/master/commons/src/main/java/com/navercorp/pinpoint/common/trace/TraceMetadataProvider.java)
`ProfilerPlugin` is used by Pinpoint Agent only while `TraceMetadataProvider` is used by Pinpoint Agent, Collector and Web.

Pinpoint loads these implementations by Java's [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html). So an plugin JAR must contains two provider-configuration files.

* META-INF/services/com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin
* META-INF/services/com.navercorp.pinpoint.common.trace.TraceMetadataProvider 

Each file should contains fully qualified names of the implementation classes.


### TraceMetadataProvider
A TraceMetadataProvider adds [ServiceType](https://github.com/naver/pinpoint/blob/master/commons/src/main/java/com/navercorp/pinpoint/common/trace/ServiceType.java)s and [AnnotationKey](https://github.com/naver/pinpoint/blob/master/commons/src/main/java/com/navercorp/pinpoint/common/trace/AnnotationKey.java)s to Pinpoint.

Both ServiceType and AnnotationKey's code value must be unique. If you're writing a private plugin, you can use code values reserved for private usage. Pinpoint will not assign these values to anything. Otherwise you have to contact Pinpoint dev team to allocate codes for the plugin. 

* ServiceType codes for private use
  * Server: 1900 ~ 1999
  * DB client: 2900 ~ 2999
  * Cache client: 8999 ~ 8999
  * RPC client: 9900 ~ 9999
  * Others: 7500 ~ 7999

* AnnotaionKey codes for private use
  * 900 ~ 999
