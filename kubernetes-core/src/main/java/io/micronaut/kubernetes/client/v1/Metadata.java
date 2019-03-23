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

package io.micronaut.kubernetes.client.v1;

import java.util.Map;

/**
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#metadata">Metadata</a>
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class Metadata {

    private String name;
    private String uid;
    private Map<String, String> labels;

    /**
     *
     * @param uid a unique in time and space value
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     *
     * @param name A string that uniquely identifies this object within the current namespace.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return  A string that uniquely identifies this object within the current namespace.
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return a unique in time and space value used to distinguish between objects with the same name that have been deleted and recreated.
     */
    public String getUid() {
        return uid;
    }

    /**
     *
     * @return A map of string keys and values that can be used to organize and categorize objects.
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     *
     * @param labels A map of string keys and values that can be used to organize and categorize objects.
     */
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "name='" + name + '\'' +
                ", uid='" + uid + '\'' +
                ", labels=" + labels +
                '}';
    }

    public boolean isSecure() {
        //TODO check labels
        return true;
    }
}
