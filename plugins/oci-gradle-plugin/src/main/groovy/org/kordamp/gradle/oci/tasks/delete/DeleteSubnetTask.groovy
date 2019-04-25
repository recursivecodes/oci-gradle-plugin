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
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.DeleteSubnetRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import groovy.transform.CompileStatic
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.oci.tasks.AbstractOCITask
import org.kordamp.gradle.oci.tasks.interfaces.OCITask
import org.kordamp.gradle.oci.tasks.traits.CompartmentIdAwareTrait
import org.kordamp.gradle.oci.tasks.traits.SubnetIdAwareTrait
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
class DeleteSubnetTask extends AbstractOCITask implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    SubnetIdAwareTrait,
    WaitForCompletionAwareTrait {
    static final String TASK_DESCRIPTION = 'Deletes a subnet.'

    private final Property<String> subnetName = project.objects.property(String)

    @Optional
    @Input
    @Option(option = 'subnet-name', description = 'The name of the subnet (REQUIRED if subnetId = null).')
    void setSubnetName(String subnetName) {
        this.subnetName.set(subnetName)
    }

    String getSubnetName() {
        return subnetName.orNull
    }

    @TaskAction
    void executeTask() {
        if (isBlank(getSubnetId()) && isBlank(getSubnetName())) {
            throw new IllegalStateException("Missing value for either 'subnetId' or 'subnetName' in $path")
        }

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        VirtualNetworkClient subnetClient = new VirtualNetworkClient(provider)

        if (isNotBlank(getSubnetId())) {
            Subnet subnet = subnetClient.getSubnet(GetSubnetRequest.builder()
                .subnetId(getSubnetId())
                .build())
                .subnet

            if (subnet) {
                setSubnetName(subnet.displayName)
                println("Deleting subnet '${subnet.displayName}' with id ${subnet.id}")
                subnetClient.deleteSubnet(DeleteSubnetRequest.builder()
                    .subnetId(getSubnetId())
                    .build())

                if (isWaitForCompletion()) {
                    println("Waiting for subnet to be Terminated")
                    subnetClient.waiters
                        .forSubnet(GetSubnetRequest.builder().subnetId(subnet.id).build(),
                            Subnet.LifecycleState.Terminated)
                        .execute()
                }
            }
        } else {
            validateCompartmentId()
            validateVcnId()

            subnetClient.listSubnets(ListSubnetsRequest.builder()
                .compartmentId(getCompartmentId())
                .vcnId(getVcnId())
                .displayName(getSubnetName())
                .build())
                .items.each { subnet ->
                setSubnetId(subnet.id)
                println("Deleting subnet '${subnet.displayName}' with id ${subnet.id}")

                subnetClient.deleteSubnet(DeleteSubnetRequest.builder()
                    .subnetId(subnet.id)
                    .build())

                if (isWaitForCompletion()) {
                    println("Waiting for subnet to be Terminated")
                    subnetClient.waiters
                        .forSubnet(GetSubnetRequest.builder().subnetId(subnet.id).build(),
                            Subnet.LifecycleState.Terminated)
                        .execute()
                }
            }
        }

        subnetClient.close()
    }
}