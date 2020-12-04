package com.navercorp.pinpoint.plugin.rocketmq;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.AnnotationKeyMatchers;
import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * @author zhangjb
 */
public class RocketMQClientTraceMetadataProvider implements TraceMetadataProvider {

    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(RocketMQClientConstants.ROCKETMQ_CONSUMER, AnnotationKeyMatchers.exact(AnnotationKey.MESSAGE_QUEUE_URI));
        context.addServiceType(RocketMQClientConstants.ROCKETMQ_PRODUCER, AnnotationKeyMatchers.exact(AnnotationKey.MESSAGE_QUEUE_URI));

        context.addAnnotationKey(RocketMQClientConstants.ROCKETMQ_MESSAGE);
    }

}
