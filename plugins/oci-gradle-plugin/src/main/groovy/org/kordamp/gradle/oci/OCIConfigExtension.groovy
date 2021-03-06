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
package org.kordamp.gradle.oci

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class OCIConfigExtension {
    Property<String> userId
    Property<String> tenantId
    Property<String> fingerprint
    Property<String> region
    RegularFileProperty keyfile
    Property<String> passphrase

    OCIConfigExtension(Project project) {
        userId = project.objects.property(String)
        tenantId = project.objects.property(String)
        fingerprint = project.objects.property(String)
        region = project.objects.property(String)
        keyfile = project.objects.fileProperty()
        passphrase = project.objects.property(String)
    }

    boolean isEmpty() {
        !userId.present &&
            !tenantId.present &&
            !fingerprint.present &&
            !region.present &&
            !keyfile.present
    }
}
