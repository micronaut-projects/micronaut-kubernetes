/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client.graalvm;


import com.oracle.svm.core.annotate.AutomaticFeature;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Configures native image generation.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@AutomaticFeature
@Internal
public class KubernetesClientFeature implements Feature {

    static final boolean DEBUG_OUTPUT = false;

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Set<Class<?>> reflectiveAccess = new HashSet<>();

        Arrays.asList(
                "io.kubernetes.client.util.Watch$Response",
                "io.kubernetes.client.custom.Quantity$QuantityAdapter",
                "io.kubernetes.client.custom.IntOrString$IntOrStringAdapter").forEach(cls -> {
            Class<?> c = access.findClassByName(cls);
            reflectiveAccess.add(c);
        });

        String[] classes = new String[0];
        try {
            Set<String> classesList = getClassNamesFromPackage(access.getApplicationClassLoader(),
                    "io.kubernetes.client.openapi.models");
            classes = classesList.toArray(new String[0]);
        } catch (IOException e) {
            System.out.println("Failed to load K8s client models: " + e);
        }

        for (String aClass : classes) {
            Class<?> c = access.findClassByName(aClass);
            reflectiveAccess.add(c);
        }

        for (Class<?> type : reflectiveAccess) {
            boolean hasNoArgsConstructor = !type.isEnum() &&
                    !type.isInterface() &&
                    hasNoArgsConstructor(type.getDeclaredConstructors());

            RuntimeReflection.register(type);
            if (hasNoArgsConstructor) {
                RuntimeReflection.registerForReflectiveInstantiation(type);
            }
            for (Method declaredMethod : type.getDeclaredMethods()) {
                RuntimeReflection.register(declaredMethod);
            }
            if (!type.isInterface()) {
                for (Field declaredField : type.getDeclaredFields()) {
                    RuntimeReflection.register(declaredField);
                }
            }
        }
    }

    private static Set<String> getClassNamesFromPackage(ClassLoader classLoader, String pkg)
            throws IOException {

        Set<String> names = new HashSet<>();

        String packageName = pkg.replace(".", "/");
        Enumeration<URL> resources = classLoader.getResources(packageName);
        while (resources.hasMoreElements()) {
            URL packageURL = resources.nextElement();
            if (DEBUG_OUTPUT) {
                System.out.println("For " + pkg + " found " + packageURL);
            }

            if (packageURL.getProtocol().equals("jar")) {
                String jarFileName;
                JarFile jf;
                Enumeration<JarEntry> jarEntries;
                String entryName;
                jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
                jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
                if (DEBUG_OUTPUT) {
                    System.out.println("Loading classes from jar " + jarFileName);
                }
                jf = new JarFile(jarFileName);
                jarEntries = jf.entries();
                while (jarEntries.hasMoreElements()) {
                    entryName = jarEntries.nextElement().getName();
                    if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                        entryName = entryName.substring(packageName.length() + 1, entryName.lastIndexOf('.'));
                        if (isKubernetesClientObject(entryName)) {
                            names.add(pkg + "." + entryName);
                            if (DEBUG_OUTPUT) {
                                System.out.println("Added " + pkg + "." + entryName);
                            }
                        }
                    }
                }
                jf.close();
            } else {
                URI uri = URI.create(packageURL.toString());
                File folder = new File(uri.getPath());
                File[] contenuti = folder.listFiles();
                if (contenuti == null) {
                    if (DEBUG_OUTPUT) {
                        System.out.println("No files to load found in " + folder.getPath());
                    }
                    return names;
                }
                String entryName;
                for (File actual : contenuti) {
                    entryName = actual.getName();
                    entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                    if (isKubernetesClientObject(entryName)) {
                        names.add(pkg + "." + entryName);
                        if (DEBUG_OUTPUT) {
                            System.out.println("Added " + pkg + "." + entryName);
                        }
                    }
                }
            }
        }
        return names;
    }

    private static boolean isKubernetesClientObject(String classname) {
        return Stream.of(
                "Fluent",
                "Builder"
        ).noneMatch(classname::contains);
    }

    private boolean hasNoArgsConstructor(Constructor<?>[] declaredConstructors) {
        boolean hasNoArgsConstructor = false;
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (declaredConstructor.getParameterCount() == 0) {
                hasNoArgsConstructor = true;
                break;
            }
        }
        return hasNoArgsConstructor;
    }
}
