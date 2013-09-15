/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.config.scala

import scala.collection.JavaConversions._

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringSetProperty(property: String, default: Set[String], delimiterRegex: String) {
  private val prop = new com.netflix.config.DynamicStringSetProperty(property, default, delimiterRegex)

  def apply: Option[Set[String]] = Option(get)

  def get: Set[String] = prop.get.toSet

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}