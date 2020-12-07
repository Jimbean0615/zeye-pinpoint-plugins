package com.navercorp.pinpoint.plugin.thread;

import com.navercorp.pinpoint.common.trace.*;

/**
 * @author echo
 */
public class ThreadConstants {

    public static final String SCOPE_NAME = "THREAD_ASYNC";

    public static final ServiceType SERVICE_TYPE = ServiceTypeFactory.of(6001, SCOPE_NAME);

    public static final AnnotationKey ZEYE_TRACEID_ANNOTATION_KEY = AnnotationKeyFactory.of(919, "zeye.traceId", new AnnotationKeyProperty[]{AnnotationKeyProperty.VIEW_IN_RECORD_SET});
    public static final String FIELD_ZEYE_TRACEID = "traceId";
    public static final String ZEYE_TRACEID_GETTER = "com.navercorp.pinpoint.plugin.thread.ZeyeTraceIdGetter";

}
