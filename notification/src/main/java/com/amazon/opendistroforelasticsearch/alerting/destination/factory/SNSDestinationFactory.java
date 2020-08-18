/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.alerting.destination.factory;

import com.amazon.opendistroforelasticsearch.alerting.destination.credentials.CredentialsProviderFactory;
import com.amazon.opendistroforelasticsearch.alerting.destination.credentials.ExpirableCredentialsProviderFactory;
import com.amazon.opendistroforelasticsearch.alerting.destination.credentials.InternalAuthCredentialsClient;
import com.amazon.opendistroforelasticsearch.alerting.destination.credentials.InternalAuthCredentialsClientPool;
import com.amazon.opendistroforelasticsearch.alerting.destination.message.SNSMessage;
import com.amazon.opendistroforelasticsearch.alerting.destination.response.DestinationHttpResponse;
import com.amazon.opendistroforelasticsearch.alerting.destination.util.Util;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Strings;

import java.util.HashMap;
import java.util.Map;

import static com.amazon.opendistroforelasticsearch.alerting.destination.util.Util.SNS_ODFE_SUPPORT;

/**
 * This class handles the clients responsible for submitting the sns messages.
 * It caches the credentials for the IAM roles, so that it can be reused.
 * <p>
 * This class fetches credentials provider from different sources(based on priority) and uses
 * the first one that works.
 */

final class SNSDestinationFactory implements DestinationFactory<SNSMessage, AmazonSNS> {
    private static final Logger logger = LogManager.getLogger(SNSDestinationFactory.class);

    private final InternalAuthCredentialsClient internalApiCredentialsClient;
    private final CredentialsProviderFactory[] orderedCredentialsProviderSources;

    /*
     * Mapping between IAM roleArn and SNSClientHelper. Each role will have its own credentials.
     */
//    Map<String, SNSClientHelper> roleClientMap = new HashMap<>();

    Map<String, SNSClientHelper> keyClientMap = new HashMap<>();

    SNSDestinationFactory() {
        this.internalApiCredentialsClient = InternalAuthCredentialsClientPool
                .getInstance()
                .getInternalAuthClient(getClass().getName());
        this.orderedCredentialsProviderSources = getOrderedCredentialsProviderSources();
    }

    /**
     * @param message
     * @return SNSResponse
     */
    @Override
    public DestinationHttpResponse publish(SNSMessage message) {
        try {
            AmazonSNS snsClient = getClient(message);
            PublishResult result;
            if (!Strings.isNullOrEmpty(message.getSubject())) {
                result = snsClient.publish(message.getTopicArn(), message.getMessage(), message.getSubject());
            } else {
                result = snsClient.publish(message.getTopicArn(), message.getMessage());
            }
            logger.info("Message successfully published: " + result.getMessageId());
            return new DestinationHttpResponse.Builder().withResponseContent(result.getMessageId())
                    .withStatusCode(result.getSdkHttpMetadata().getHttpStatusCode()).build();
        } catch (Exception ex) {
            logger.error("Exception publishing Message for SNS: " + message.toString(), ex);
            throw ex;
        }
    }

    /**
     * Fetches the client corresponding to an IAM role
     *
     * @param message sns message
     * @return AmazonSNS AWS SNS client
     */
    @Override
    public AmazonSNS getClient(SNSMessage message) {

        String roleArn = message.getRoleArn();
        String accessKey = message.getIAMAccessKey().toString();
        String secretKey = message.getIAMSecretKey().toString();

        String key = SNS_ODFE_SUPPORT ? String.join("|", accessKey, secretKey) : roleArn;

        if (!keyClientMap.containsKey(key)) {
            AWSCredentialsProvider credentialsProvider = SNS_ODFE_SUPPORT ? getProvider(accessKey, secretKey) : getProvider(roleArn);
            keyClientMap.put(roleArn, new SNSClientHelper(credentialsProvider));
        }

        AmazonSNS snsClient = keyClientMap.get(roleArn).getSnsClient(Util.getRegion(message.getTopicArn()));
        return snsClient;
    }

    /**
     * @param roleArn
     * @return AWSCredentialsProvider
     * @throws IllegalArgumentException
     */
    public AWSCredentialsProvider getProvider(String roleArn) throws IllegalArgumentException {
        AWSCredentialsProvider credentialsProvider;

        for (CredentialsProviderFactory providerSource : orderedCredentialsProviderSources) {
            credentialsProvider = providerSource.getProvider(roleArn);

            if (credentialsProvider != null) {
                return credentialsProvider;
            }
        }
        // no credential provider present
        return null;
    }

    /**
     * @return AWSCredentialsProvider
     * @throws IllegalArgumentException
     */
    public AWSCredentialsProvider getProvider(String accessKey, String secretKey) throws IllegalArgumentException {

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return new AWSStaticCredentialsProvider(awsCredentials);
    }

    private CredentialsProviderFactory[] getOrderedCredentialsProviderSources() {
        return new CredentialsProviderFactory[]{
                // currently we are just supporting internal credential provider factory, going forward we would
                // support multiple provider factories. We can mention the order in which the credential provdier
                // can be picked up here
                new ExpirableCredentialsProviderFactory(internalApiCredentialsClient)
        };
    }
}

/**
 * This helper class caches the credentials for a role and creates client
 * for each AWS region based on the topic ARN
 */
class SNSClientHelper {
    private AWSCredentialsProvider credentialsProvider;
    // Map between Region and client
    private Map<String, AmazonSNS> snsClientMap = new HashMap();

    SNSClientHelper(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public AmazonSNS getSnsClient(String region) {
        if (!snsClientMap.containsKey(region)) {
            AmazonSNS snsClient = AmazonSNSClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(credentialsProvider).build();
            snsClientMap.put(region, snsClient);
        }
        return snsClientMap.get(region);
    }
}
