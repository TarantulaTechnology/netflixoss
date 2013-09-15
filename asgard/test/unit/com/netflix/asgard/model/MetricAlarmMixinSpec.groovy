/*
 * Copyright 2012 Netflix, Inc.
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
package com.netflix.asgard.model

import com.amazonaws.services.cloudwatch.model.MetricAlarm
import spock.lang.Specification

class MetricAlarmMixinSpec extends Specification {

    def setup() {
        MetricAlarm.mixin MetricAlarmMixin
    }

    def 'should display alarm info'() {
        MetricAlarm alarm = new MetricAlarm(metricName: 'CpuLoad', comparisonOperator: 'GreaterThanThreshold',
                threshold: 50)
        expect:
        alarm.toDisplayValue() == 'CpuLoad > 50.0'
    }

    def 'should display alarm info with decimal'() {
        MetricAlarm alarm = new MetricAlarm(metricName: 'CpuLoad', comparisonOperator: 'LessThanOrEqualToThreshold',
                threshold: 10.5)
        expect:
        alarm.toDisplayValue() == 'CpuLoad <= 10.5'
    }

}

