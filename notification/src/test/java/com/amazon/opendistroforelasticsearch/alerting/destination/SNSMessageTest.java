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

package com.amazon.opendistroforelasticsearch.alerting.destination;

import com.amazon.opendistroforelasticsearch.alerting.destination.message.SNSMessage;
import com.amazon.opendistroforelasticsearch.alerting.destination.util.Util;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.easymock.EasyMock;
import org.elasticsearch.common.settings.SecureString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SNSMessageTest {

    @Test(expected = AmazonSNSException.class)
    public void testSNSMessage() throws Exception {
        AmazonSNSClient amazonSNSClient = EasyMock.createMock(AmazonSNSClient.class);
        PublishResult result = new PublishResult();
        result.setMessageId("messageId");
        EasyMock.expect(
                amazonSNSClient.publish(EasyMock.anyObject(PublishRequest.class))).andReturn(
                result);
        EasyMock.replay(amazonSNSClient);

        SNSMessage message = new SNSMessage.Builder("sns")
                .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification")
                .withIAMSecretKey(new SecureString("accessKey"))
                .withIAMAccessKey(new SecureString("secretKey"))
                .withRole("arn:aws:iam::853806060000:role/domain/abc")
                .withMessage("Hello")
                .build();
        Notification.publish(message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRoleArnMissingMessage() {
        try {
            Util.SNS_ODFE_SUPPORT = false;
            SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage").build();
        } catch (Exception ex) {
            assertEquals("Role arn is missing/invalid: null", ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTopicArnMissingMessage() {
        try {
            Util.SNS_ODFE_SUPPORT = false;
            SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage")
                    .withRole("arn:aws:iam::853806060000:role/domain/abc").build();
        } catch (Exception ex) {
            assertEquals("Topic arn is missing/invalid: null", ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSecretKeyMissingMessage() {
        try {
            Util.SNS_ODFE_SUPPORT = true;
            SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage")
                    .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification")
                    .withIAMAccessKey(new SecureString("arn:aws:iam::853806060000:role/domain/abc")).build();
        } catch (Exception ex) {
            assertEquals("IAM user secret key is missing", ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAccessKeyMissingMessage() {
        try {
            Util.SNS_ODFE_SUPPORT = true;
            SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage")
                    .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification")
                    .withIAMSecretKey(new SecureString("arn:aws:iam::853806060000:role/domain/abc")).build();
        } catch (Exception ex) {
            assertEquals("IAM user access key is missing", ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateContentMissingMessage() {
        try {
            SNSMessage message = new SNSMessage.Builder("sms")
                    .withRole("arn:aws:iam::853806060000:role/domain/abc")
                    .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification").build();
        } catch (Exception ex) {
            assertEquals("Message content is missing", ex.getMessage());
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInValidRoleMessage() {
        try {
            SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage")
                    .withRole("dummyRole")
                    .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification").build();
        } catch (Exception ex) {
            assertEquals("Role arn is missing/invalid: dummyRole", ex.getMessage());
            throw ex;
        }
    }

    @Test
    public void testValidMessage() {
        SNSMessage message = new SNSMessage.Builder("sms").withMessage("dummyMessage")
                .withRole("arn:aws:iam::853806060000:role/domain/abc")
                .withIAMSecretKey(new SecureString("arn:aws:iam::853806060000:role/domain/abc"))
                .withIAMAccessKey(new SecureString("arn:aws:iam::853806060000:role/domain/abc"))
                .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification").build();
        assertEquals("sms", message.getChannelName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInValidChannelName() {
        try {
            SNSMessage message = new SNSMessage.Builder("").withMessage("dummyMessage")
                    .withRole("arn:aws:iam::853806060000:role/domain/abc")
                    .withTopicArn("arn:aws:sns:us-west-2:475313751589:test-notification").build();
        } catch (Exception ex) {
            assertEquals("Channel name must be defined", ex.getMessage());
            throw ex;
        }
    }
}

