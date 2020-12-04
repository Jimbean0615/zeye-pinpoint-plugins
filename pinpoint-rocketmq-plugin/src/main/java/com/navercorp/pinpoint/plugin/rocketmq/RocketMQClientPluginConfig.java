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

import com.navercorp.pinpoint.bootstrap.config.ExcludePathFilter;
import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.SkipFilter;
import com.navercorp.pinpoint.common.util.StringUtils;

/**
 * @author 微风
 */
public class RocketMQClientPluginConfig {

    public static final String DEFAULT_DESTINATION_PATH_SEPARATOR = ".";

    private final boolean traceRocketMQClient;
    private final boolean traceRocketMQClientProducer;
    private final boolean traceRocketMQClientConsumer;
    private final boolean traceRocketMQTextMessage;
    private final Filter<String> excludeDestinationFilter;

    public RocketMQClientPluginConfig(ProfilerConfig config) {
        this.traceRocketMQClient = config.readBoolean("profiler.rocketmq.client.enable", true);
        this.traceRocketMQClientProducer = config.readBoolean("profiler.rocketmq.client.producer.enable", true);
        this.traceRocketMQClientConsumer = config.readBoolean("profiler.rocketmq.client.consumer.enable", true);
        this.traceRocketMQTextMessage = config.readBoolean("profiler.rocketmq.client.trace.message", false);
        String excludeDestinationPathSeparator = config.readString("profiler.rocketmq.client.destination.separator", DEFAULT_DESTINATION_PATH_SEPARATOR);
        if (StringUtils.isEmpty(excludeDestinationPathSeparator)) {
            excludeDestinationPathSeparator = DEFAULT_DESTINATION_PATH_SEPARATOR;
        }
        String excludeDestinations = config.readString("profiler.rocketmq.client.destination.exclude", "");
        if (!excludeDestinations.isEmpty()) {
            this.excludeDestinationFilter = new ExcludePathFilter(excludeDestinations, excludeDestinationPathSeparator);
        } else {
            this.excludeDestinationFilter = new SkipFilter<String>();
        }
    }

    public boolean isTraceRocketMQClient() {
        return this.traceRocketMQClient;
    }

    public boolean isTraceRocketMQClientProducer() {
        return this.traceRocketMQClientProducer;
    }

    public boolean isTraceRocketMQClientConsumer() {
        return this.traceRocketMQClientConsumer;
    }

    public boolean isTraceRocketMQTextMessage() {
        return traceRocketMQTextMessage;
    }

    public Filter<String> getExcludeDestinationFilter() {
        return this.excludeDestinationFilter;
    }

}