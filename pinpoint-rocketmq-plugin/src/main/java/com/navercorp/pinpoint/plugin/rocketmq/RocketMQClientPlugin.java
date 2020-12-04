package com.navercorp.pinpoint.plugin.rocketmq;

import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matcher;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matchers;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.operand.InterfaceInternalNameMatcherOperand;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageConsumerReceiveInterceptorV2;
import com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor;
import com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageConsumerReceiveInterceptor;
import com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageProducerSendInterceptor;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.producer.MessageQueueSelector;

import java.security.ProtectionDomain;
import java.util.List;

/**
 * @author zhangjb
 */
@SuppressWarnings("all")
public class RocketMQClientPlugin implements ProfilerPlugin, MatchableTransformTemplateAware {

    private static final PLogger logger = PLoggerFactory.getLogger(RocketMQClientPlugin.class);
    private static Filter<String> excludeDestinationFilter = null;

    private MatchableTransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        logger.info("load rocketmq plugin");
        RocketMQClientPluginConfig config = new RocketMQClientPluginConfig(context.getConfig());
        if (!config.isTraceRocketMQClient()) {
            logger.info("load rocketmq plugin return");
            return;
        }
        if (config.isTraceRocketMQClientConsumer() || config.isTraceRocketMQClientProducer()) {
            excludeDestinationFilter = config.getExcludeDestinationFilter();
            if (config.isTraceRocketMQClientProducer()) {
                logger.info("load rocketmq plugin producer");
                this.addProducerEditor();
            }
            if (config.isTraceRocketMQClientConsumer()) {
                logger.info("load rocketmq plugin consumer");
                this.addConsumerEditor();
            }
        }
    }

    private void addProducerEditor() {

        /**
         * zmq使用的ONS普通消息
         */
        transformTemplate.transform("com.aliyun.openservices.ons.api.impl.rocketmq.ProducerImpl", OnsProducerTransformCallback.class);

        /**
         * zmq使用的ONS顺序消息
         */
        transformTemplate.transform("com.aliyun.openservices.ons.api.impl.rocketmq.OrderProducerImpl", OnsOrderProducerTransformCallback.class);

        /**
         * zmq使用的ONS事务消息
         */
        transformTemplate.transform("com.aliyun.openservices.ons.api.impl.rocketmq.TransactionProducerImpl", OnsTransactionProducerTransformCallback.class);

        /**
         * zmq使用的RocketMQ普通消息和顺序消息
         */
        transformTemplate.transform("org.apache.rocketmq.client.producer.DefaultMQProducer", RocketMQProducerTransformCallback.class);
    }

    private void addConsumerEditor() {
        // 对以下目录进行扫描
        String[] packages = {"cn.gov.zcy", "com.zcy", "com.dtdream", "io.terminus", "xin.cai"};
        for (String basePackageName : packages) {
            /**
             * ONS普通消息消费
             */
            Matcher matcher1 = Matchers.newPackageBasedMatcher(basePackageName, new InterfaceInternalNameMatcherOperand("com.aliyun.openservices.ons.api.MessageListener", true));
            transformTemplate.transform(matcher1, OnsConsumerTransformCallback.class);

            /**
             * ONS顺序消息消费
              */
            Matcher matcher2 = Matchers.newPackageBasedMatcher(basePackageName, new InterfaceInternalNameMatcherOperand("com.aliyun.openservices.ons.api.order.MessageOrderListener", true));
            transformTemplate.transform(matcher2, OnsOrderConsumerTransformCallback.class);

            /**
             * RocketMQ普通消息消费
             */
            Matcher matcher3 = Matchers.newPackageBasedMatcher(basePackageName, new InterfaceInternalNameMatcherOperand("org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently", true));
            transformTemplate.transform(matcher3, RocketMQConsumerTransformCallback.class);

            /**
             * RocketMQ顺序消息消费
             */
            Matcher matcher4 = Matchers.newPackageBasedMatcher(basePackageName, new InterfaceInternalNameMatcherOperand("org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly", true));
            transformTemplate.transform(matcher4, RocketMQOrderConsumerTransformCallback.class);
        }
    }

    // TransformCallbackClass must be public static inner class
    public static class OnsProducerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            logger.info("OnsProducerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");

            /**
             * zmq发送ONS普通消息用的是这个方法
             * @see com.aliyun.openservices.ons.api.impl.rocketmq.ProducerImpl#send(Message)
             */
            InstrumentMethod method = target.getDeclaredMethod("send", "com.aliyun.openservices.ons.api.Message");
            try {
                method.addScopedInterceptor(OnsMessageProducerSendInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unsupported method " + method, e);
                }
            }

            logger.info("OnsProducerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");
            return target.toBytecode();
        }
    }

    public static class OnsOrderProducerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            logger.info("OnsOrderProducerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");

            /**
             * zmq发送ONS顺序消息使用的是这个方法
             * @see com.aliyun.openservices.ons.api.impl.rocketmq.OrderProducerImpl#send(Message, String)
             */
            InstrumentMethod method = target.getDeclaredMethod("send", "com.aliyun.openservices.ons.api.Message", "java.lang.String");
            try {
                method.addScopedInterceptor(OnsMessageProducerSendInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unsupported method " + method, e);
                }
            }
            logger.info("OnsOrderProducerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");

            return target.toBytecode();
        }
    }

    public static class OnsTransactionProducerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            logger.info("OnsTransactionProducerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");

            /**
             * zmq发送ONS事务消息使用的是这个方法
             * @see com.aliyun.openservices.ons.api.impl.rocketmq.TransactionProducerImpl#send(Message, LocalTransactionExecuter, Object)
             */
            InstrumentMethod method = target.getDeclaredMethod("send", "com.aliyun.openservices.ons.api.Message", "com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter", "java.lang.Object");
            try {
                method.addScopedInterceptor(OnsMessageProducerSendInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unsupported method " + method, e);
                }
            }
            logger.info("OnsTransactionProducerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageProducerSendInterceptor");

            return target.toBytecode();
        }
    }

    public static class  RocketMQProducerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> aClass, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            logger.info("DefaultMQProducer find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageProducerSendInterceptor");

            /**
             * zmq发送RocketMQ普通消息使用的是这个方法
             * @see org.apache.rocketmq.client.producer.DefaultMQProducer#send(org.apache.rocketmq.common.message.Message)
             */
            InstrumentMethod method = target.getDeclaredMethod("send", "org.apache.rocketmq.common.message.Message");
            try {
                method.addScopedInterceptor(RocketMQMessageProducerSendInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unsupported method " + method, e);
                }
            }

            /**
             * zmq发送RocketMQ顺序消息使用的是这个方法
             * @see org.apache.rocketmq.client.producer.DefaultMQProducer#send(org.apache.rocketmq.common.message.Message, MessageQueueSelector, Object)
             */
            method = target.getDeclaredMethod("send", "org.apache.rocketmq.common.message.Message", "org.apache.rocketmq.client.producer.MessageQueueSelector", "java.lang.Object");
            try {
                method.addScopedInterceptor(RocketMQMessageProducerSendInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Unsupported method " + method, e);
                }
            }
            logger.info("DefaultMQProducer add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageProducerSendInterceptor");
            return target.toBytecode();
        }
    }


    public static class OnsConsumerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            logger.info("OnsConsumerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageConsumerReceiveInterceptorV2");

            /**
             * zmq使用的ons普通消息消费方法
             * @see com.aliyun.openservices.ons.api.MessageListener#consume(Message, ConsumeContext)
             */
            final InstrumentMethod consumeMessageMethod = target.getDeclaredMethod("consume", "com.aliyun.openservices.ons.api.Message", "com.aliyun.openservices.ons.api.ConsumeContext");
            if (consumeMessageMethod != null) {
                consumeMessageMethod.addScopedInterceptor(OnsMessageConsumerReceiveInterceptorV2.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
                logger.info("OnsConsumerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageConsumerReceiveInterceptorV2");
            }

            return target.toBytecode();
        }
    }

    public static class OnsOrderConsumerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            logger.info("OnsOrderConsumerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageConsumerReceiveInterceptorV2");

            /**
             * zmq使用的ons顺序消息消费方法
             * @see com.aliyun.openservices.ons.api.order.MessageOrderListener#consume(Message, ConsumeOrderContext)
             */
            final InstrumentMethod consumeMessageMethod = target.getDeclaredMethod("consume", "com.aliyun.openservices.ons.api.Message", "com.aliyun.openservices.ons.api.order.ConsumeOrderContext");
            if (consumeMessageMethod != null) {
                consumeMessageMethod.addScopedInterceptor(OnsMessageConsumerReceiveInterceptorV2.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
                logger.info("OnsOrderConsumerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.OnsMessageConsumerReceiveInterceptorV2");
            }

            return target.toBytecode();
        }
    }

    public static class RocketMQConsumerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            logger.info("RocketMQConsumerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageConsumerReceiveInterceptor");

            /**
             * zmq使用的rocketmq普通消息消费方法
             * @see org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently#consumeMessage(List, ConsumeConcurrentlyContext)
             */
            final InstrumentMethod consumeMessageMethod = target.getDeclaredMethod("consumeMessage", "java.util.List", "org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext");
            if (consumeMessageMethod != null) {
                consumeMessageMethod.addScopedInterceptor(RocketMQMessageConsumerReceiveInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
                logger.info("RocketMQConsumerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageConsumerReceiveInterceptor");
            }

            return target.toBytecode();
        }
    }

    public static class RocketMQOrderConsumerTransformCallback implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            logger.info("RocketMQOrderConsumerTransformCallback find Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageConsumerReceiveInterceptor");

            /**
             * zmq使用的rocketmq顺序消息消费方法
             * @see org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly#consumeMessage(List, ConsumeOrderlyContext)
             */
            final InstrumentMethod consumeMessageMethod = target.getDeclaredMethod("consumeMessage", "java.util.List", "org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext");
            if (consumeMessageMethod != null) {
                consumeMessageMethod.addScopedInterceptor(RocketMQMessageConsumerReceiveInterceptor.class, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
                logger.info("RocketMQOrderConsumerTransformCallback add Interceptor com.navercorp.pinpoint.plugin.rocketmq.interceptor.RocketMQMessageConsumerReceiveInterceptor");
            }

            return target.toBytecode();
        }
    }

    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
