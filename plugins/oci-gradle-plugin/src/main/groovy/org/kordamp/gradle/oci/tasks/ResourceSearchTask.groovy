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

import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.resourcesearch.ResourceSearch
import com.oracle.bmc.resourcesearch.ResourceSearchClient
import com.oracle.bmc.resourcesearch.model.QueryableFieldDescription
import com.oracle.bmc.resourcesearch.model.ResourceType
import com.oracle.bmc.resourcesearch.requests.GetResourceTypeRequest
import com.oracle.bmc.resourcesearch.requests.ListResourceTypesRequest
import com.oracle.bmc.resourcesearch.responses.GetResourceTypeResponse
import com.oracle.bmc.resourcesearch.responses.ListResourceTypesResponse
import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.kordamp.gradle.AnsiConsole

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class ResourceSearchTask extends AbstractOCITask {
    static final String NAME = 'searchResources'
    static final String DESCRIPTION = 'Lists information on resource types'

    private static final String CONFIG_LOCATION = '~/.oci/config'

    private String type

    @Option(option = 'type', description = 'The type to search (optional).')
    void setType(String type) {
        this.type = type
    }

    @TaskAction
    void search() {
        ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(CONFIG_LOCATION, profile)
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile)

        ResourceSearch client = ResourceSearchClient.builder().build(provider)
        if (isBlank(type)) {
            listTypes(client)
        } else {
            getTypeDetails(client, type)
        }
    }

    private void listTypes(ResourceSearch client) {
        ListResourceTypesRequest request = ListResourceTypesRequest.builder().build()
        ListResourceTypesResponse response = client.listResourceTypes(request)

        AnsiConsole console = new AnsiConsole(project)
        println('Total resources: ' + console.cyan(response.items.size().toString()))
        for (ResourceType type : response.items) {
            println('Resource: ' + console.yellow(type.name))
        }
    }

    private void getTypeDetails(ResourceSearch client, String typeName) {
        GetResourceTypeRequest request = GetResourceTypeRequest.builder().name(typeName).build()
        GetResourceTypeResponse response = client.getResourceType(request)

        AnsiConsole console = new AnsiConsole(project)
        println('Resource: ' + console.yellow(response.resourceType.name))
        println('fields:')
        doPrint(console, response.resourceType.fields, 0)
    }

    @Override
    protected void doPrint(AnsiConsole console, Object value, int offset) {
        if (value instanceof QueryableFieldDescription) {
            doPrintQueryableFieldDescription(console, (QueryableFieldDescription) value, offset)
        } else {
            super.doPrint(console, value, offset)
        }
    }

    @Override
    protected void doPrintElement(AnsiConsole console, Object value, int offset) {
        if (value instanceof QueryableFieldDescription) {
            doPrintQueryableFieldDescription(console, (QueryableFieldDescription) value, offset)
        } else {
            super.doPrintElement(console, value, offset)
        }
    }

    private void doPrintQueryableFieldDescription(AnsiConsole console, QueryableFieldDescription desc, int offset) {
        println(('    ' * (offset + 1)) + desc.fieldName + ':')
        doPrintMapEntry(console, 'type', desc.fieldType, offset + 2)
        // doPrintMapEntry(console, 'isArray', desc.isArray, offset + 2)
        if (desc.objectProperties?.size()) {
            println(('    ' * (offset + 2)) + 'fields:')
            doPrint(console, desc.objectProperties, offset + 2)
        }
    }
}
