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
package org.kordamp.gradle.oci.tasks

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.kordamp.gradle.AnsiConsole
import org.kordamp.gradle.oci.tasks.traits.CompartmentAwareTrait
import org.kordamp.gradle.oci.tasks.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ListAvailabilityDomainsTask extends AbstractOCITask implements CompartmentAwareTrait, VerboseAwareTrait {
    static final String NAME = 'listAvailabilityDomains'
    static final String DESCRIPTION = 'Lists domains available on a compartment'

    @TaskAction
    void listAvailabilityDomains() {
        validateCompartmentId()

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        IdentityClient client = IdentityClient.builder().build(provider)
        ListAvailabilityDomainsResponse response = client.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder().compartmentId(compartmentId).build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println("Total availability domains available at ${compartmentId}: " + console.cyan(response.items.size().toString()))
        println(' ')
        for (AvailabilityDomain domain : response.items) {
            println(domain.name + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, domain, 0)
            }
        }
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof AvailabilityDomain) {
            printAvailabilityDomainDetails(console, (AvailabilityDomain) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof AvailabilityDomain) {
            printAvailabilityDomainDetails(console, (AvailabilityDomain) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void printAvailabilityDomainDetails(AnsiConsole console, AvailabilityDomain domain, int offset) {
        doPrintMapEntry(console, 'Id', domain.id, offset + 1)
    }
}
