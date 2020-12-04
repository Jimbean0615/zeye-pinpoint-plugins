/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.rocketmq;

import com.navercorp.pinpoint.common.trace.*;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

/**
 * @author 微风
 * @author zhangjb
 */
public final class RocketMQClientConstants {
    private RocketMQClientConstants() {
    }

    public static final ServiceType ROCKETMQ_CONSUMER = ServiceTypeFactory.of(1997, "ROCKETMQ_CONSUMER", RECORD_STATISTICS);
    public static final ServiceType ROCKETMQ_PRODUCER = ServiceTypeFactory.of(1996, "ROCKETMQ_PRODUCER", RECORD_STATISTICS);

    public static final AnnotationKey ROCKETMQ_MESSAGE = AnnotationKeyFactory.of(124, "rocketmq.message", AnnotationKeyProperty.VIEW_IN_RECORD_SET);

    public static final String ROCKETMQ_CLIENT_SCOPE = "RocketMQClientScope";
    public static final String ZEYE_TRACE_ID_KEY = "traceId";
}
