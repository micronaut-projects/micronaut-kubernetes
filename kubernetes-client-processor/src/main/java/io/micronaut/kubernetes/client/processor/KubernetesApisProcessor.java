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
import io.micronaut.annotation.processing.AnnotationUtils;
import io.micronaut.annotation.processing.GenericUtils;
import io.micronaut.annotation.processing.ModelUtils;
import io.micronaut.annotation.processing.PublicMethodVisitor;
import io.micronaut.annotation.processing.visitor.JavaVisitorContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.naming.NameUtils;

import javax.annotation.processing.*;

import io.micronaut.inject.visitor.TypeElementVisitor;
import jakarta.inject.Singleton;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An annotation processor that generates the Kubernetes APIs factories. Based on {@code io.micronaut.kubernetes.client.Apis}
 * annotation field {@code kind} either {@code Async}, {@code RxJava2} or {@code Reactor} client factories are generated.
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
    private Elements elements;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final Set<? extends Element> element = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element e : element) {
                final List<String> apisNames = resolveClientNames(e);
                final String t = resolveClientType(e);
                for (String clientName : apisNames) {
                    final String packageName = NameUtils.getPackageName(clientName);
                    final String simpleName = NameUtils.getSimpleName(clientName);
                    switch (t) {
                        case "RXJAVA2":
                            writeRxJava2Clients(e, packageName, simpleName);
                            break;
                        case "RXJAVA3":
                            writeRxJava3Clients(e, packageName, simpleName);
                            break;
                        case "REACTOR":
                            writeReactorClients(e, packageName, simpleName);
                            break;
                        case "ASYNC":
                            writeClientFactory(e, packageName, simpleName);
                            break;
                        default:
                            break;
                    }
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

        if (Objects.equals(simpleName, "CoreV1Api")) {
            builder.addAnnotation(BootstrapContextCompatible.class);
            buildMethod.addAnnotation(BootstrapContextCompatible.class);
        }

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

    private void writeReactorClients(Element e, String packageName, String simpleName) {
        final String reactorClientName = simpleName + "ReactorClient";
        final String reactorPackageName = packageName.replace(KUBERNETES_APIS_PACKAGE, MICRONAUT_APIS_PACKAGE + ".reactor");

        ClassName cn = ClassName.get(reactorPackageName, reactorClientName);
        TypeSpec.Builder builder = TypeSpec.classBuilder(cn);

        ClassName clientType = ClassName.get(packageName, simpleName);
        ClassName reactorMonoType = ClassName.get("reactor.core.publisher", "Mono");
        final AnnotationSpec.Builder requiresSpec =
                AnnotationSpec.builder(Requires.class)
                        .addMember("beans", "{$T.class}", clientType);

        builder.addAnnotation(requiresSpec.build());
        builder.addAnnotation(Singleton.class);

        if (Objects.equals(simpleName, "CoreV1Api")) {
            builder.addAnnotation(BootstrapContextCompatible.class);
        }

        builder.addModifiers(Modifier.PUBLIC);
        builder.addField(clientType, "client", Modifier.FINAL, Modifier.PRIVATE);
        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(clientType, "client")
                .addCode("this.client = client;")
                .build());

        TypeElement typeElement = elements.getTypeElement(clientType.reflectionName());
        if (typeElement != null) {
            ModelUtils modelUtils = new ModelUtils(elements, types) {
            };
            GenericUtils genericUtils = new GenericUtils(elements, types, modelUtils) {
            };
            AnnotationUtils annotationUtils = new AnnotationUtils(processingEnv, elements, messager, types, modelUtils, genericUtils, filer) {
            };
            JavaVisitorContext visitorContext = new JavaVisitorContext(
                    processingEnv,
                    messager,
                    elements,
                    annotationUtils,
                    types,
                    modelUtils,
                    genericUtils,
                    filer,
                    MutableConvertibleValues.of(new LinkedHashMap<>()),
                    TypeElementVisitor.VisitorKind.ISOLATING);
            typeElement.asType().accept(new PublicMethodVisitor<Object, Object>(visitorContext) {
                @Override
                protected void accept(DeclaredType type, Element element, Object o) {
                    ExecutableElement ee = (ExecutableElement) element;
                    TypeMirror returnType = ee.getReturnType();
                    if (element.getSimpleName().toString().endsWith("Async")) {
                        DeclaredType dt = (DeclaredType) returnType;
                        Element e = dt.asElement();
                        if (e.getSimpleName().toString().equals("Call")) {
                            List<? extends VariableElement> parameters = ee.getParameters();
                            VariableElement fieldElement = parameters.get(parameters.size() - 1);
                            TypeMirror typeMirror = fieldElement.asType();
                            if (typeMirror instanceof DeclaredType) {
                                DeclaredType cdt = (DeclaredType) typeMirror;
                                List<? extends TypeMirror> typeArguments = cdt.getTypeArguments();
                                if (typeArguments.size() == 1) {
                                    TypeMirror ctm = typeArguments.get(0);
                                    if (ctm instanceof DeclaredType) {
                                        // resolve the callback response type
                                        TypeName responseType = ClassName.get(ctm);

                                        // resolve the method name by removing the Async suffix
                                        String methodName = ee.getSimpleName().toString();
                                        String finalMethodName = methodName.replace("Async", "");

                                        // prepare parameters for the method without tha _callback one
                                        List<ParameterSpec> parameterSpecs = parameters.stream()
                                                .filter(va -> !va.getSimpleName().toString().equals("_callback"))
                                                .filter(va -> !va.getSimpleName().toString().equals("watch"))
                                                .map(va -> ParameterSpec.builder(ClassName.get(va.asType()), va.getSimpleName().toString()).build())
                                                .collect(Collectors.toList());

                                        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(finalMethodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameters(parameterSpecs)
                                                .returns(
                                                        ParameterizedTypeName.get(
                                                                reactorMonoType,
                                                                responseType
                                                        )
                                                );

                                        methodBuilder.addCode(CodeBlock.builder()
                                                .addStatement("return $T.create((sink) -> {", reactorMonoType)
                                                .addStatement("try {")
                                                .add("    this.client." + methodName + "(")
                                                .add(parameters.stream()
                                                        .map(va -> {
                                                            String name = va.getSimpleName().toString();
                                                            if (name.equals("_callback")) {
                                                                return "new AsyncCallbackSink<" + responseType + ">(sink)";
                                                            } else if (name.equals("watch")) {
                                                                return "Boolean.FALSE";
                                                            } else {
                                                                return name;
                                                            }
                                                        })
                                                        .collect(Collectors.joining(", ")))
                                                .addStatement(")")
                                                .addStatement("} catch(io.kubernetes.client.openapi.ApiException e) { sink.error(e); }")
                                                .addStatement("})")
                                                .build());
                                        builder.addMethod(methodBuilder.build());
                                    }
                                }
                            }
                        }
                    }
                }
            }, null);
        }

        writeJavaFile(e, reactorPackageName, cn, builder);
    }

    private void writeJavaFile(Element e, String reactorPackageName, ClassName cn, TypeSpec.Builder builder) {
        final JavaFile javaFile = JavaFile.builder(reactorPackageName, builder.build()).build();
        try {
            final JavaFileObject javaFileObject = filer.createSourceFile(cn.reflectionName(), e);
            try (Writer writer = javaFileObject.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (IOException ioException) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Oracle SDK factories: " + ioException.getMessage(), e);
        }
    }

    private void writeRxJava2Clients(Element e, String packageName, String simpleName) {
        writeRxJavaClients(e, packageName, simpleName, ".rxjava2", "io.reactivex");
    }

    private void writeRxJava3Clients(Element e, String packageName, String simpleName) {
        writeRxJavaClients(e, packageName, simpleName, ".rxjava3", "io.reactivex.rxjava3.core");
    }

    private void writeRxJavaClients(Element e, String packageName, String simpleName, String rxVersionSuffix, String rxJavaPackage) {
        final String rx = simpleName + "RxClient";

        final String rxPackageName = packageName.replace(KUBERNETES_APIS_PACKAGE, MICRONAUT_APIS_PACKAGE + rxVersionSuffix);

        ClassName cn = ClassName.get(rxPackageName, rx);
        TypeSpec.Builder builder = TypeSpec.classBuilder(cn);

        ClassName clientType = ClassName.get(packageName, simpleName);
        ClassName rxSingleType = ClassName.get(rxJavaPackage, "Single");


        final AnnotationSpec.Builder requiresSpec =
                AnnotationSpec.builder(Requires.class)
                        .addMember("beans", "{$T.class}", clientType);
        builder.addAnnotation(requiresSpec.build());
        builder.addAnnotation(Singleton.class);
        builder.addModifiers(Modifier.PUBLIC);
        builder.addField(clientType, "client", Modifier.FINAL, Modifier.PRIVATE);
        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(clientType, "client")
                .addCode("this.client = client;")
                .build());

        TypeElement typeElement = elements.getTypeElement(clientType.reflectionName());
        if (typeElement != null) {
            ModelUtils modelUtils = new ModelUtils(elements, types) {
            };
            GenericUtils genericUtils = new GenericUtils(elements, types, modelUtils) {
            };
            AnnotationUtils annotationUtils = new AnnotationUtils(processingEnv, elements, messager, types, modelUtils, genericUtils, filer) {
            };
            JavaVisitorContext visitorContext = new JavaVisitorContext(
                    processingEnv,
                    messager,
                    elements,
                    annotationUtils,
                    types,
                    modelUtils,
                    genericUtils,
                    filer,
                    MutableConvertibleValues.of(new LinkedHashMap<>()),
                    TypeElementVisitor.VisitorKind.ISOLATING);
            typeElement.asType().accept(new PublicMethodVisitor<Object, Object>(visitorContext) {
                @Override
                protected void accept(DeclaredType type, Element element, Object o) {
                    ExecutableElement ee = (ExecutableElement) element;
                    TypeMirror returnType = ee.getReturnType();
                    if (element.getSimpleName().toString().endsWith("Async")) {
                        DeclaredType dt = (DeclaredType) returnType;
                        Element e = dt.asElement();
                        if (e.getSimpleName().toString().equals("Call")) {
                            List<? extends VariableElement> parameters = ee.getParameters();
                            VariableElement fieldElement = parameters.get(parameters.size() - 1);
                            TypeMirror typeMirror = fieldElement.asType();
                            if (typeMirror instanceof DeclaredType) {
                                DeclaredType cdt = (DeclaredType) typeMirror;
                                List<? extends TypeMirror> typeArguments = cdt.getTypeArguments();
                                if (typeArguments.size() == 1) {
                                    TypeMirror ctm = typeArguments.get(0);
                                    if (ctm instanceof DeclaredType) {
                                        // resolve the callback response type
                                        TypeName responseType = ClassName.get(ctm);

                                        // resolve the method name by removing the Async suffix
                                        String methodName = ee.getSimpleName().toString();
                                        String finalMethodName = methodName.replace("Async", "");

                                        // prepare parameters for the method without tha _callback one
                                        List<ParameterSpec> parameterSpecs = parameters.stream()
                                                .filter(va -> !va.getSimpleName().toString().equals("_callback"))
                                                .filter(va -> !va.getSimpleName().toString().equals("watch"))
                                                .map(va -> ParameterSpec.builder(ClassName.get(va.asType()), va.getSimpleName().toString()).build())
                                                .collect(Collectors.toList());

                                        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(finalMethodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameters(parameterSpecs)
                                                .returns(
                                                        ParameterizedTypeName.get(
                                                                rxSingleType,
                                                                responseType
                                                        )
                                                );

                                        methodBuilder.addCode(CodeBlock.builder()
                                                .addStatement("return $T.create((emitter) -> {", rxSingleType)
                                                .add("this.client." + methodName + "(")
                                                .add(parameters.stream()
                                                        .map(va -> {
                                                            String name = va.getSimpleName().toString();
                                                            if (name.equals("_callback")) {
                                                                return "new ApiCallbackEmitter<>(emitter)";
                                                            } else if (name.equals("watch")) {
                                                                return "Boolean.FALSE";
                                                            } else {
                                                                return name;
                                                            }
                                                        })
                                                        .collect(Collectors.joining(", ")))
                                                .addStatement(")")
                                                .addStatement("})")
                                                .build());
                                        builder.addMethod(methodBuilder.build());
                                    }
                                }
                            }
                        }
                    }
                }
            }, null);
        }

        writeJavaFile(e, rxPackageName, cn, builder);

    }

    private List<String> resolveClientNames(Element e) {
        List<String> clientNames = new ArrayList<>();
        final List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            TypeElement te = (TypeElement) annotationMirror.getAnnotationType().asElement();
            String ann = te.getSimpleName().toString();
            if (ann.equals("Apis")) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();

                AnnotationValue value = null;
                // Look for value in @Apis
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    final ExecutableElement executableElement = entry.getKey();
                    if (executableElement.getSimpleName().toString().equals("value")) {
                        value = entry.getValue();
                        break;
                    }
                }

                // If not get value from default
                if (value == null) {
                    for (Element element : annotationMirror.getAnnotationType().asElement().getEnclosedElements()) {
                        if (element instanceof ExecutableElement) {
                            final ExecutableElement exEl = (ExecutableElement) element;
                            if (exEl.getSimpleName().toString().equals("value")) {
                                value = exEl.getDefaultValue();
                                break;
                            }
                        }
                    }
                }

                if (value != null) {
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
        return clientNames;
    }

    private String resolveClientType(Element e) {
        final List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            TypeElement te = (TypeElement) annotationMirror.getAnnotationType().asElement();
            String ann = te.getSimpleName().toString();
            if (ann.equals("Apis")) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    final ExecutableElement executableElement = entry.getKey();
                    if (executableElement.getSimpleName().toString().equals("kind")) {
                        final AnnotationValue value = entry.getValue();
                        final Object v = value.getValue();
                        if (v != null) {
                            return v.toString();
                        }
                    }
                }
            }
        }
        return "ASYNC";
    }
}
