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
package org.kordamp.gradle.oci.tasks.instance

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.requests.TerminateInstanceRequest
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInstanceIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.OptionalInstanceNameAwareTrait
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
class TerminateInstanceTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    OptionalInstanceIdAwareTrait,
    OptionalInstanceNameAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Terminates an Instance.'

    @TaskAction
    void executeTask() {
        validateInstanceId()

        if (isBlank(getInstanceId()) && isBlank(getInstanceName())) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        ComputeClient client = new ComputeClient(provider)

        // TODO: check if instance exists
        // TODO: check is instance is in a 'deletable' state

        if (isNotBlank(getInstanceId())) {
            Instance instance = client.getInstance(GetInstanceRequest.builder()
                .instanceId(getInstanceId())
                .build())
                .instance

            if (instance) {
                setInstanceName(instance.displayName)
                terminateInstance(client, instance)
            }
        } else {
            validateCompartmentId()

            client.listInstances(ListInstancesRequest.builder()
                .compartmentId(compartmentId)
                .displayName(getInstanceName())
                .build())
                .items.each { instance ->
                setInstanceId(instance.id)
                terminateInstance(client, instance)
            }
        }

        client.close()
    }

    private void terminateInstance(ComputeClient client, Instance instance) {
        println("Terminating Instance '${instance.displayName}' with id ${instance.id}")
        client.terminateInstance(TerminateInstanceRequest.builder()
            .instanceId(instance.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Instance to be Terminated")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    Instance.LifecycleState.Terminated)
                .execute()
        }
    }
}
