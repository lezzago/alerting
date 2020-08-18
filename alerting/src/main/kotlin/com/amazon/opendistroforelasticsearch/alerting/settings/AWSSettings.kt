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

package com.amazon.opendistroforelasticsearch.alerting.settings

import com.amazon.opendistroforelasticsearch.alerting.destination.util.Util
import org.elasticsearch.common.settings.SecureSetting
import org.elasticsearch.common.settings.SecureString
import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings
import java.io.IOException

data class AWSSettings(

    val snsIamUser: SecureString,
    val snsIamPassword: SecureString,
    val isOnAWS: Boolean
) {
    companion object {
        val SNS_IAM_USER_ACCESS_KEY = SecureSetting.secureString(
                "opendistro.alerting.destination.sns.access.key",
                null
        )

        val SNS_IAM_USER_SECRET_KEY = SecureSetting.secureString(
                "opendistro.alerting.destination.sns.secret.key",
                null
        )

        val SNS_ODFE_SUPPORT = Setting.boolSetting("opendistro.alerting.destination.sns.enabled", false, Setting.Property.NodeScope)

        @JvmStatic
        @Throws(IOException::class)
        fun parse(settings: Settings): AWSSettings {
            Util.SNS_ODFE_SUPPORT = SNS_ODFE_SUPPORT.get(settings)
            return AWSSettings(
                    SNS_IAM_USER_ACCESS_KEY.get(settings),
                    SNS_IAM_USER_SECRET_KEY.get(settings),
                    SNS_ODFE_SUPPORT.get(settings)
            )
        }
    }
}
