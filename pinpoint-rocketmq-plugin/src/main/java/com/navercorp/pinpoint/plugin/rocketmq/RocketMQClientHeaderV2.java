/*
 * Copyright 2016 Naver Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.rocketmq;


import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author 微风
 */
public enum RocketMQClientHeaderV2 {
    /**
     *
     */
    ROCKETMQ_TRACE_ID("Pinpoint-TraceID"),
    /**
     *
     */
    ROCKETMQ_SPAN_ID("Pinpoint-SpanID"),
    /**
     *
     */
    ROCKETMQ_PARENT_SPAN_ID("Pinpoint-pSpanID"),
    /**
     *
     */
    ROCKETMQ_SAMPLED("Pinpoint-Sampled"),
    /**
     *
     */
    ROCKETMQ_FLAGS("Pinpoint-Flags"),
    /**
     *
     */
    ROCKETMQ_PARENT_APPLICATION_NAME("Pinpoint-pAppName"),
    /**
     *
     */
    ROCKETMQ_PARENT_APPLICATION_TYPE("Pinpoint-pAppType"),
    /**
     *
     */
    ZEYE_TRACE_ID("traceId");

    private final String id;

    RocketMQClientHeaderV2(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    private interface MessageHandler {
        void setMessage(MessageExt message, RocketMQClientHeaderV2 key, Object value);

        String getMessage(MessageExt message, RocketMQClientHeaderV2 key);
    }

    private static final MessageHandler messageHandlerBase = new MessageHandler() {

        @Override
        public final void setMessage(MessageExt message, RocketMQClientHeaderV2 key, Object value) {
            message.putUserProperty(key.id, value.toString());
        }

        @Override
        public final String getMessage(MessageExt message, RocketMQClientHeaderV2 key) {
            String id = key.id;
            String userProperties = message.getUserProperty(id);
            return userProperties;
        }
    };

    public static void setTraceId(MessageExt message, String traceId) {
        messageHandlerBase.setMessage(message, ROCKETMQ_TRACE_ID, traceId);
    }

    public static String getTraceId(MessageExt message, String defaultValue) {
        return messageHandlerBase.getMessage(message, ROCKETMQ_TRACE_ID);
    }

    public static void setZeyeTraceId(MessageExt message, String traceId) {
        messageHandlerBase.setMessage(message, ZEYE_TRACE_ID, traceId);
    }

    public static String getZeyeTraceId(MessageExt message) {
        return messageHandlerBase.getMessage(message, ZEYE_TRACE_ID);
    }

    public static void setSpanId(MessageExt message, Long spanId) {
        messageHandlerBase.setMessage(message, ROCKETMQ_SPAN_ID, spanId);
    }

    public static Long getSpanId(MessageExt message, Long defaultValue) {

        String value = messageHandlerBase.getMessage(message, ROCKETMQ_SPAN_ID);
        if(value == null) {
            return defaultValue;
        }
        return Long.valueOf(value);
    }

    public static void setParentSpanId(MessageExt message, Long parentSpanId) {
        messageHandlerBase.setMessage(message, ROCKETMQ_PARENT_SPAN_ID, parentSpanId);
    }

    public static Long getParentSpanId(MessageExt message, Long defaultValue) {
        String value = messageHandlerBase.getMessage(message, ROCKETMQ_PARENT_SPAN_ID);
        if(value == null) {
            return defaultValue;
        }
        return Long.valueOf(value);
    }

    public static void setSampled(MessageExt message, Boolean sampled) {
        messageHandlerBase.setMessage(message, ROCKETMQ_SAMPLED, sampled);
    }

    public static Boolean getSampled(MessageExt message, Boolean defaultValue) {
        String value = messageHandlerBase.getMessage(message, ROCKETMQ_SAMPLED);
        if(value == null) {
            return defaultValue;
        }
        return Boolean.valueOf(value);
    }

    public static void setFlags(MessageExt message, Short flags) {
        messageHandlerBase.setMessage(message, ROCKETMQ_FLAGS, flags);
    }

    public static Short getFlags(MessageExt message, Short defaultValue) {
        String value =  messageHandlerBase.getMessage(message, ROCKETMQ_FLAGS);
        if(value == null) {
            return defaultValue;
        }
        return Short.valueOf(value);
    }

    public static void setParentApplicationName(MessageExt message, String parentApplicationName) {
        messageHandlerBase.setMessage(message, ROCKETMQ_PARENT_APPLICATION_NAME, parentApplicationName);
    }

    public static String getParentApplicationName(MessageExt message, String defaultValue) {
        return messageHandlerBase.getMessage(message, ROCKETMQ_PARENT_APPLICATION_NAME);
    }

    public static void setParentApplicationType(MessageExt message, Short parentApplicationType) {
        messageHandlerBase.setMessage(message, ROCKETMQ_PARENT_APPLICATION_TYPE, parentApplicationType);
    }

    public static Short getParentApplicationType(MessageExt message, Short defaultValue) {
        String value = messageHandlerBase.getMessage(message, ROCKETMQ_PARENT_APPLICATION_TYPE);
        if(value == null) {
            return defaultValue;
        }
        return Short.valueOf(value);
    }

}
