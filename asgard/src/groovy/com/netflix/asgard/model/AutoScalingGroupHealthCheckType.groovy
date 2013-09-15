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

enum AutoScalingGroupHealthCheckType {
    EC2('Replace terminated instances'),
    ELB('Replace instances that fail ELB health check')

    String description

    AutoScalingGroupHealthCheckType(String description) {
        this.description = description
    }

    static String ensureValidType(String typeName) {
        if (typeName && values().collect { it.name() }.contains(typeName)) { return typeName }
        EC2
    }

    static AutoScalingGroupHealthCheckType by(String value) {
        values().find { it.name() == value }
    }

    String toString() {
        this.name()
    }
}
