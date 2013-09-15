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
package com.netflix.asgard

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

/**
 * Static utilities for Spring-related infrastructure tasks such as autowiring instance variables.
 */
class Spring {

    /**
     * Autowires spring service singleton instances for
     *
     * @param object the custom object that contains instance variables to autowire by name
     */
    static void autowire(def object) {
        ApplicationHolder.application?.mainContext?.autowireCapableBeanFactory?.autowireBeanProperties(
            object, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
    }
}
