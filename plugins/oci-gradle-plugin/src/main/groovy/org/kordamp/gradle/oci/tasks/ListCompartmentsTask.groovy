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
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import com.oracle.bmc.identity.responses.ListCompartmentsResponse
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
class ListCompartmentsTask extends AbstractOCITask implements CompartmentAwareTrait, VerboseAwareTrait {
    static final String NAME = 'listCompartments'
    static final String DESCRIPTION = 'Lists available compartments'

    @TaskAction
    void listCompartments() {
        validateCompartmentId()

        AuthenticationDetailsProvider provider = resolveAuthenticationDetailsProvider()
        IdentityClient client = IdentityClient.builder().build(provider)
        ListCompartmentsResponse response = client.listCompartments(ListCompartmentsRequest.builder()
            .compartmentId(compartmentId)
            .build())
        client.close()

        AnsiConsole console = new AnsiConsole(project)
        println("Total compartments available at ${compartmentId}: " + console.cyan(response.items.size().toString()))
        println(' ')
        for (Compartment compartment : response.items) {
            println(compartment.name + (verbose ? ':' : ''))
            if (verbose) {
                doPrint(console, compartment, 0)
            }
        }
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof Compartment) {
            printCompartmentDetails(console, (Compartment) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof Compartment) {
            printCompartmentDetails(console, (Compartment) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void printCompartmentDetails(AnsiConsole console, Compartment compartment, int offset) {
        doPrintMapEntry(console, 'Id', compartment.id, offset + 1)
        doPrintMapEntry(console, 'Compartment Id', compartment.compartmentId, offset + 1)
        doPrintMapEntry(console, 'Name', compartment.name, offset + 1)
        doPrintMapEntry(console, 'Description', compartment.description, offset + 1)
        doPrintMapEntry(console, 'Time Created', compartment.timeCreated, offset + 1)
    }
}
