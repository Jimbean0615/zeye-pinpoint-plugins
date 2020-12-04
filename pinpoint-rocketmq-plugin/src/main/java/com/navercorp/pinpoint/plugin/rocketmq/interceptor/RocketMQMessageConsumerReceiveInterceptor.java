package com.navercorp.pinpoint.plugin.rocketmq.interceptor;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanRecursiveAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.rocketmq.HostUtil;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQClientConstants;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQClientHeaderV2;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;

import java.util.List;

/**
 * @author zhangjb
 */
public class RocketMQMessageConsumerReceiveInterceptor extends SpanRecursiveAroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;

    public RocketMQMessageConsumerReceiveInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        recorder.recordServiceType(RocketMQClientConstants.ROCKETMQ_CONSUMER);

    }

    @Override
    protected Trace createTrace(Object target, Object[] args) {
        List<MessageExt> messages = (List)args[0];
        MessageExt message = messages.get(0);
        // 注入zeye traceId，不受采样率限制
        final String zeyeTraceId = RocketMQClientHeaderV2.getZeyeTraceId(message);
        logger.info("RocketMQMessageConsumerReceiveInterceptor createTrace zeyeTraceId: {}", zeyeTraceId);
        if (zeyeTraceId != null && !"".equals(zeyeTraceId)) {
            MDC.put("traceId", zeyeTraceId);
        }
        final TraceId traceId = populateTraceIdFromRequest(message);
        final Trace trace = traceId == null ? traceContext.newTraceObject() : traceContext.continueTraceObject(traceId);
        if (trace.canSampled()) {
            final SpanRecorder recorder = trace.getSpanRecorder();
            // You have to record a service type within Server range.
            recorder.recordServiceType(RocketMQClientConstants.ROCKETMQ_CONSUMER);
            recordRequest(recorder, target, args);
        }
        return trace;
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {

        List<MessageExt> messages = (List)args[0];
        MessageExt message = messages.get(0);
        recorder.recordApi(descriptor, args);
        if (throwable == null) {

            recorder.recordAttribute(RocketMQClientConstants.ROCKETMQ_MESSAGE, new String(message.getBody()));
        } else {
            recorder.recordException(throwable);
        }
    }
    private TraceId populateTraceIdFromRequest(MessageExt message) {
        String transactionId = RocketMQClientHeaderV2.getTraceId(message, null);
        if (transactionId == null) {
            return null;
        }
        long parentSpanId = RocketMQClientHeaderV2.getParentSpanId(message, SpanId.NULL);
        long spanId = RocketMQClientHeaderV2.getSpanId(message, SpanId.NULL);
        short flags = RocketMQClientHeaderV2.getFlags(message, (short) 0);
        return traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);
    }


    private void recordRequest(SpanRecorder recorder, Object target, Object[] args) {
        List<MessageExt> messages = (List)args[0];
        MessageExt message = messages.get(0);
        String endPoint = message.getTopic() + "(" + message.getMsgId() + ","  + message.getTags() + "," +  message.getKeys() + "," + message.getReconsumeTimes() + ")";
        recorder.recordRpcName(endPoint);
        recorder.recordEndPoint(endPoint);

        // Record rpc name, client address, server address.
        String host = HostUtil.getLocalHost();
        recorder.recordRemoteAddress(host);
        recorder.recordAcceptorHost(host);

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            final String parentApplicationName = RocketMQClientHeaderV2.getParentApplicationName(message, ServiceType.UNDEFINED.getName());
            if (parentApplicationName != null) {
                final short parentApplicationType = RocketMQClientHeaderV2.getParentApplicationType(message, ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);
            }
        }
    }

}
