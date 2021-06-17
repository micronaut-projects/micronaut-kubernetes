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
package io.micronaut.kubernetes.client.processor;

import com.squareup.javapoet.*;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.naming.NameUtils;

import javax.annotation.processing.*;
import jakarta.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An annotation processor that generates the Kubernetes APIs factories.
 *
 * @author Pavol Gressa
 * @since 2.2
 */
@SupportedAnnotationTypes("io.micronaut.kubernetes.client.Apis")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class KubernetesApisProcessor extends AbstractProcessor {

    public static final String KUBERNETES_APIS_PACKAGE = "io.kubernetes.client.openapi.apis";
    public static final String MICRONAUT_APIS_PACKAGE = "io.micronaut.kubernetes.client";

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final Set<? extends Element> element = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element e : element) {
                List<String> apisNames = resolveClientNames(e);
                for (String clientName : apisNames) {
                    final String packageName = NameUtils.getPackageName(clientName);
                    final String simpleName = NameUtils.getSimpleName(clientName);
                    writeClientFactory(e, packageName, simpleName);
                }
            }
        }
        return false;
    }

    private void writeClientFactory(Element e, String packageName, String simpleName) {
        final String factoryName = simpleName + "Factory";
        final String factoryPackageName = packageName.replace(KUBERNETES_APIS_PACKAGE, MICRONAUT_APIS_PACKAGE);
        final TypeSpec.Builder builder = TypeSpec.classBuilder(factoryName);
        builder.addAnnotation(Factory.class);

        final MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build");
        buildMethod.returns(ClassName.get(packageName, simpleName))
                .addParameter(ClassName.get("io.kubernetes.client.openapi", "ApiClient"), "apiClient")
                .addAnnotation(Singleton.class)
                .addModifiers(Modifier.PROTECTED)
                .addCode("return new " + simpleName + "(apiClient);");
        builder.addMethod(buildMethod.build());


        final JavaFile javaFile = JavaFile.builder(factoryPackageName, builder.build()).build();
        try {
            final JavaFileObject javaFileObject = filer.createSourceFile(factoryPackageName + "." + factoryName, e);
            try (Writer writer = javaFileObject.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (IOException ioException) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Kubernetes " + simpleName + "  factory: " + ioException.getMessage(), e);
        }
    }

    private List<String> resolveClientNames(Element e) {
        List<String> clientNames = new ArrayList<>();
        final List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            TypeElement te = (TypeElement) annotationMirror.getAnnotationType().asElement();
            String ann = te.getSimpleName().toString();
            if (ann.equals("Apis")) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    final ExecutableElement executableElement = entry.getKey();
                    if (executableElement.getSimpleName().toString().equals("value")) {
                        final AnnotationValue value = entry.getValue();
                        final Object v = value.getValue();
                        if (v instanceof Iterable) {
                            Iterable<Object> i = (Iterable) v;
                            for (Object o : i) {
                                if (o instanceof AnnotationValue) {
                                    final Object nested = ((AnnotationValue) o).getValue();
                                    if (nested instanceof DeclaredType) {
                                        final TypeElement dte = (TypeElement) ((DeclaredType) nested).asElement();
                                        clientNames.add(dte.getQualifiedName().toString());
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return clientNames;
    }
}
