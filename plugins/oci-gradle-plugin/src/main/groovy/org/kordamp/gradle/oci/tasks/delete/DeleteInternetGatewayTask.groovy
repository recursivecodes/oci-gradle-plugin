/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
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
package org.kordamp.gradle.oci.tasks.delete

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.requests.DeleteInternetGatewayRequest
import com.oracle.bmc.core.requests.GetInternetGatewayRequest
import com.oracle.bmc.core.requests.ListInternetGatewaysRequest
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInternetGatewayIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInternetGatewayNameAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VcnIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.WaitForCompletionAwareTrait
import org.kordamp.jipsy.TypeProviderFor

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@TypeProviderFor(OCITask)
class DeleteInternetGatewayTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    OptionalInternetGatewayIdAwareTrait,
    OptionalInternetGatewayNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a InternetGateway.'

    @TaskAction
    void executeTask() {
        validateInternetGatewayId()

        if (isBlank(getInternetGatewayId()) && isBlank(getInternetGatewayName())) {
            throw new IllegalStateException("Missing value for either 'internetGatewayId' or 'internetGatewayName' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient client = new VirtualNetworkClient(provider)

        // TODO: check if gateway exists
        // TODO: check is gateway is in a 'deletable' state

        if (isNotBlank(getInternetGatewayId())) {
            InternetGateway internetGateway = client.getInternetGateway(GetInternetGatewayRequest.builder()
                .igId(getInternetGatewayId())
                .build())
                .internetGateway

            if (internetGateway) {
                setInternetGatewayName(internetGateway.displayName)
                deleteInternetGateway(client, internetGateway)
            }
        } else {
            validateCompartmentId()
            validateVcnId()

            client.listInternetGateways(ListInternetGatewaysRequest.builder()
                .compartmentId(getCompartmentId())
                .vcnId(getVcnId())
                .displayName(getInternetGatewayName())
                .build())
                .items.each { internetGateway ->
                setInternetGatewayId(internetGateway.id)
                deleteInternetGateway(client, internetGateway)
            }
        }

        client.close()
    }

    private void deleteInternetGateway(VirtualNetworkClient client, InternetGateway internetGateway) {
        println("Deleting InternetGateway '${internetGateway.displayName}' with id ${internetGateway.id}")

        client.deleteInternetGateway(DeleteInternetGatewayRequest.builder()
            .igId(internetGateway.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for InternetGateway to be Terminated")
            client.waiters
                .forInternetGateway(GetInternetGatewayRequest.builder()
                    .igId(internetGateway.id).build(),
                    InternetGateway.LifecycleState.Terminated)
                .execute()
        }

        // TODO: remove from vcn routing table
    }
}
