package com.navercorp.pinpoint.plugin.thread.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.thread.ThreadConstants;
import com.navercorp.pinpoint.plugin.thread.ZeyeTraceIdAccessor;
import org.slf4j.MDC;

/**
 * @author echo
 */
public class ThreadConstructorInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private TraceContext traceContext;
    private MethodDescriptor descriptor;

    public ThreadConstructorInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        logger.info("ThreadConstructorInterceptor before");
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        final SpanEventRecorder recorder = trace.traceBlockBegin();
        if (target instanceof AsyncContextAccessor) {
            final AsyncContext asyncContext = recorder.recordNextAsyncContext();
            ((AsyncContextAccessor) target)._$PINPOINT$_setAsyncContext(asyncContext);
        }

        if (target instanceof ZeyeTraceIdAccessor) {
            String traceId = MDC.get("traceId");
            ((ZeyeTraceIdAccessor) target)._$PINPOINT$_setZeyeTraceId(traceId);
        }

    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        logger.info("ThreadConstructorInterceptor after trace: {}", trace);
        if (trace == null) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(this.descriptor);
            recorder.recordServiceType(ThreadConstants.SERVICE_TYPE);
            recorder.recordException(throwable);
            recorder.recordAttribute(ThreadConstants.ZEYE_TRACEID_ANNOTATION_KEY, MDC.get("traceId"));
        } finally {
            trace.traceBlockEnd();
        }
    }
}
