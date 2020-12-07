package com.navercorp.pinpoint.plugin.thread.interceptor;

import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AsyncContextSpanEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.thread.ThreadConstants;
import com.navercorp.pinpoint.plugin.thread.ZeyeTraceIdAccessor;
import org.slf4j.MDC;

/**
 * @author echo
 */
public class ThreadCallInterceptor extends AsyncContextSpanEventSimpleAroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    public ThreadCallInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {
        try {
            logger.info("ThreadCallInterceptor doInBeforeTrace: {}, {}", args, target);
            if (target instanceof ZeyeTraceIdAccessor) {
                String traceId = ((ZeyeTraceIdAccessor) target)._$PINPOINT$_getZeyeTraceId();
                logger.info("ThreadCallInterceptor doInBeforeTrace Zeye traceId: {}", traceId);
                if (traceId != null && !"".equals(traceId)) {
                    MDC.put("traceId", traceId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordServiceType(ThreadConstants.SERVICE_TYPE);
        recorder.recordException(throwable);
        recorder.recordAttribute(ThreadConstants.ZEYE_TRACEID_ANNOTATION_KEY, MDC.get("traceId"));

    }
}
