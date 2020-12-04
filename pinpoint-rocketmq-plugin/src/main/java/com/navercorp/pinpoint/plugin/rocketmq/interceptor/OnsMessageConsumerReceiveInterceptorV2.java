package com.navercorp.pinpoint.plugin.rocketmq.interceptor;

import com.aliyun.openservices.ons.api.Message;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanRecursiveAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.rocketmq.HostUtil;
import com.navercorp.pinpoint.plugin.rocketmq.OnsClientHeader;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQClientConstants;
import org.slf4j.MDC;


/**
 * @author zhangjb
 */
public class OnsMessageConsumerReceiveInterceptorV2 extends SpanRecursiveAroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;

    public OnsMessageConsumerReceiveInterceptorV2(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, RocketMQClientConstants.ROCKETMQ_CLIENT_SCOPE);
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        recorder.recordServiceType(RocketMQClientConstants.ROCKETMQ_CONSUMER);

    }

    @Override
    @SuppressWarnings("all")
    protected Trace createTrace(Object target, Object[] args) {
        Message message = (Message) args[0];
        // 注入zeye traceId，不受采样率限制
        final String zeyeTraceId = OnsClientHeader.getZeyeTraceId(message);
        logger.info("OnsMessageConsumerReceiveInterceptorV2 createTrace zeyeTraceId: {}", zeyeTraceId);
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

        Message message = (Message) args[0];
        recorder.recordApi(descriptor, args);
        if (throwable == null) {

            recorder.recordAttribute(RocketMQClientConstants.ROCKETMQ_MESSAGE, new String(message.getBody()));
        } else {
            recorder.recordException(throwable);
        }
    }
    private TraceId populateTraceIdFromRequest(Message message) {
        String transactionId = OnsClientHeader.getTraceId(message, null);
        if (transactionId == null) {
            return null;
        }
        long parentSpanId = OnsClientHeader.getParentSpanId(message, SpanId.NULL);
        long spanId = OnsClientHeader.getSpanId(message, SpanId.NULL);
        short flags = OnsClientHeader.getFlags(message, (short) 0);
        return traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);
    }

    @SuppressWarnings("all")
    private void recordRequest(SpanRecorder recorder, Object target, Object[] args) {
        Message message = (Message) args[0];
        String endPoint = message.getTopic() + "(" + message.getMsgID() + "," + message.getTag() + "," + message.getKey() + "," + message.getReconsumeTimes() + ")";
        recorder.recordRpcName(endPoint);
        recorder.recordEndPoint(endPoint);

        // Record rpc name, client address, server address.
        String host = HostUtil.getLocalHost();
        recorder.recordRemoteAddress(host);
        recorder.recordAcceptorHost(host);

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            final String parentApplicationName = OnsClientHeader.getParentApplicationName(message, ServiceType.UNDEFINED.getName());
            if (parentApplicationName != null) {
                final short parentApplicationType = OnsClientHeader.getParentApplicationType(message, ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);
            }
        }
    }

}
