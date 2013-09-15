/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.exhibitor.core.s3;

public interface S3ClientFactory
{
    /**
     * Create a client with the given credentials
     *
     * @param credentials credentials
     * @param s3Region the region to use or null
     * @return client
     * @throws Exception errors
     */
    public S3Client makeNewClient(S3Credential credentials, String s3Region) throws Exception;


    /**
     * Create a client with the given credentials
     *
     * @param credentialsProvider credentials provider
     * @param s3Region the region to use or null
     * @return client
     * @throws Exception errors
     */
    public S3Client makeNewClient(S3CredentialsProvider credentialsProvider, String s3Region) throws Exception;



}
