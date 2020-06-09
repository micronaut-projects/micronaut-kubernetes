/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;

/**
 * A key-value pair in the Data field of the referenced ConfigMap that will be projected into the volume as a file whose name is the key and content is the value.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class KeyToPath {
    private String key;
    private String path;
    private Integer mode;

    /**
     * @return The key to project.
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key The key to project.
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @return The relative path of the file to map the key to. May not be an absolute path. May not contain the path element '..'. May not start with the string '..'.
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path The relative path of the file to map the key to. May not be an absolute path. May not contain the path element '..'. May not start with the string '..'.
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * @return Optional: mode bits to use on this file, must be a value between 0 and 0777. If not specified, the volume defaultMode will be used. This might be in conflict with other options that affect the file mode, like fsGroup, and the result can be other mode bits set.
     */
    public Integer getMode() {
        return mode;
    }

    /**
     * @param mode Optional: mode bits to use on this file, must be a value between 0 and 0777. If not specified, the volume defaultMode will be used. This might be in conflict with other options that affect the file mode, like fsGroup, and the result can be other mode bits set.
     */
    public void setMode(final Integer mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "KeyToPath{" +
                "key='" + key + '\'' +
                ", path='" + path + '\'' +
                ", mode=" + mode +
                '}';
    }
}
