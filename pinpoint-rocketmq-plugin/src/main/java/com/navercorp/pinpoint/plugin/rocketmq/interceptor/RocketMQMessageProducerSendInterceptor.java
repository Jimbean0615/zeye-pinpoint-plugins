package com.navercorp.pinpoint.plugin.rocketmq.interceptor;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQClientConstants;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQClientHeader;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.MDC;


/**
 * @author zhangjb
 */
public class RocketMQMessageProducerSendInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;

    public RocketMQMessageProducerSendInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        try {

            Message message = (Message) args[0];

            final Trace trace = traceContext.currentTraceObject();
            if (trace == null) {
                logger.info("RocketMQMessageProducerSendInterceptor before trade is null");
                return;
            }
            if (trace.canSampled()) {
                logger.info("RocketMQMessageProducerSendInterceptor before canSampled");

                SpanEventRecorder spanEventRecorder = trace.traceBlockBegin();
                spanEventRecorder.recordServiceType(RocketMQClientConstants.ROCKETMQ_PRODUCER);
                spanEventRecorder.recordAttribute(RocketMQClientConstants.ROCKETMQ_MESSAGE, new String(message.getBody()));
                TraceId nextId = trace.getTraceId().getNextTraceId();
                spanEventRecorder.recordNextSpanId(nextId.getSpanId());

                RocketMQClientHeader.setTraceId(message, nextId.getTransactionId());
                RocketMQClientHeader.setSpanId(message, nextId.getSpanId());
                RocketMQClientHeader.setParentSpanId(message, nextId.getParentSpanId());
                RocketMQClientHeader.setFlags(message, nextId.getFlags());
                RocketMQClientHeader.setParentApplicationName(message, traceContext.getApplicationName());
                RocketMQClientHeader.setParentApplicationType(message, traceContext.getServerTypeCode());
            } else {
                RocketMQClientHeader.setSampled(message, false);
            }

            // add zeye traceId，不受采样率限制
            RocketMQClientHeader.setZeyeTraceId(message, MDC.get(RocketMQClientConstants.ZEYE_TRACE_ID_KEY));
        } catch (Throwable t) {
            logger.warn("BEFORE. Cause:{}", t.getMessage(), t);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            logger.info("RocketMQMessageProducerSendInterceptor trade is null ");
            return;
        }

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor, args);
            recorder.recordServiceType(RocketMQClientConstants.ROCKETMQ_PRODUCER);
            if (throwable != null) {
                recorder.recordException(throwable);
            }
        } catch (Throwable t) {
            logger.warn("AFTER error. Cause:{}", t.getMessage(), t);
        } finally {
            trace.traceBlockEnd();
        }

    }

}
