/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.client.v1.pods;

import java.util.List;

import io.micronaut.core.annotation.Introspected;

/**
 * A source of content for a container volume backed by a Secret.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class SecretVolumeSource {

    private String secretName;
    private boolean optional;
    private int defaultMode;
    private List<KeyToPath> items;

    /**
     * @return Name of the secret in the pod's namespace to use.
     */
    public String getSecretName() {
        return secretName;
    }

    /**
     * @param secretName Name of the secret in the pod's namespace to use.
     */
    public void setSecretName(final String secretName) {
        this.secretName = secretName;
    }

    /**
     * @return Specifies whether the Secret or its keys must be defined
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional Whether the Secret or its keys must be defined
     */
    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    /**
     * @return Optional: mode bits to use on created files by default. Must be a value between 0 and 0777. Defaults to 0644. Directories within the path are not affected by this setting.
     */
    public int getDefaultMode() {
        return defaultMode;
    }

    /**
     * @param defaultMode Optional: mode bits to use on created files by default. Must be a value between 0 and 0777. Defaults to 0644. Directories within the path are not affected by this setting.
     */
    public void setDefaultMode(final int defaultMode) {
        this.defaultMode = defaultMode;
    }

    /**
     * @return If unspecified, each key-value pair in the Data field of the referenced Secret will be projected into the volume as a file whose name is the key and content is the value. If specified, the listed keys will be projected into the specified paths, and unlisted keys will not be present. If a key is specified which is not present in the Secret, the volume setup will error unless it is marked optional. Paths must be relative and may not contain the '..' path or start with '..'.
     */
    public List<KeyToPath> getItems() {
        return items;
    }

    /**
     * @param items If unspecified, each key-value pair in the Data field of the referenced Secret will be projected into the volume as a file whose name is the key and content is the value. If specified, the listed keys will be projected into the specified paths, and unlisted keys will not be present. If a key is specified which is not present in the Secret, the volume setup will error unless it is marked optional. Paths must be relative and may not contain the '..' path or start with '..'.
     */
    public void setItems(final List<KeyToPath> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "SecretVolumeSource{" +
                "secretName='" + secretName + '\'' +
                ", optional=" + optional +
                ", defaultMode=" + defaultMode +
                ", items=" + items +
                '}';
    }
}
