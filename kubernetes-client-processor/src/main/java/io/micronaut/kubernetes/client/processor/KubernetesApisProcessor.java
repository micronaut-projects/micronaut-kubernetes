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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.micronaut.annotation.processing.JavaElementAnnotationMetadataFactory;
import io.micronaut.annotation.processing.ModelUtils;
import io.micronaut.annotation.processing.visitor.JavaClassElement;
import io.micronaut.annotation.processing.visitor.JavaElementFactory;
import io.micronaut.annotation.processing.visitor.JavaVisitorContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import jakarta.inject.Singleton;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private static final String MICRONAUT_APIS_PACKAGE = "io.micronaut.kubernetes.client";

    private static final ClassName REACTOR_CLASS_NAME = ClassName.get("reactor.core.publisher", "Mono");
    private static final ClassName RXJAVA2_CLASS_NAME = ClassName.get("io.reactivex", "Single");
    private static final ClassName RXJAVA3_CLASS_NAME = ClassName.get("io.reactivex.rxjava3.core", "Single");

    private static final String REACTOR_METHOD_CODE = """
        return Mono.create((sink) -> {
          try {
            request.executeAsync(new AsyncCallbackSink<>(sink));
          } catch(io.kubernetes.client.openapi.ApiException e) {
            sink.error(e);
          }
        });
        """;

    private static final String RXJAVA_METHOD_CODE = """
        return Single.create((emitter) -> {
          request.executeAsync(new ApiCallbackEmitter<>(emitter));
        });
        """;

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;
    private JavaVisitorContext javaVisitorContext;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.javaVisitorContext = new JavaVisitorContext(
            processingEnv,
            messager,
            elements,
            types,
            new ModelUtils(elements, types) { },
            filer,
            MutableConvertibleValues.of(new LinkedHashMap<>()),
            TypeElementVisitor.VisitorKind.ISOLATING,
            new HashSet<>());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                final List<String> apisNames = resolveClientNames(element);
                final ClientType clientType = resolveClientType(element);
                for (String apiName : apisNames) {
                    if (clientType == ClientType.ASYNC) {
                        writeClientFactory(element, apiName);
                    } else {
                        writeReactiveClient(element, apiName, clientType);
                    }
                }
            }
        }
        return false;
    }

    private void writeClientFactory(Element e, String apiName) {
        final String packageName = NameUtils.getPackageName(apiName);
        final String simpleName = NameUtils.getSimpleName(apiName);
        final String factoryName = simpleName + "Factory";

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

        TypeSpec factoryTypeSpec = builder.build();
        writeJavaFile(e, ClassName.get(MICRONAUT_APIS_PACKAGE, factoryName), factoryTypeSpec);
    }

    private void writeReactiveClient(Element e, String apiName, ClientType clientType) {
        final String packageName = NameUtils.getPackageName(apiName);
        final String simpleName = NameUtils.getSimpleName(apiName);

        ClassName reactiveClientClassName;
        if (clientType == ClientType.REACTOR) {
            reactiveClientClassName = ClassName.get(MICRONAUT_APIS_PACKAGE + ".reactor", simpleName + "ReactorClient");
        } else if (clientType == ClientType.RXJAVA2) {
            reactiveClientClassName = ClassName.get(MICRONAUT_APIS_PACKAGE + ".rxjava2", simpleName + "RxClient");
        } else {
            reactiveClientClassName = ClassName.get(MICRONAUT_APIS_PACKAGE + ".rxjava3", simpleName + "RxClient");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(reactiveClientClassName);

        ClassName apiClassName = ClassName.get(packageName, simpleName);
        final AnnotationSpec.Builder requiresSpec =
                AnnotationSpec.builder(Requires.class)
                        .addMember("beans", "{$T.class}", apiClassName);

        builder.addAnnotation(requiresSpec.build());
        builder.addAnnotation(Singleton.class);

        if (Objects.equals(simpleName, "CoreV1Api")) {
            builder.addAnnotation(BootstrapContextCompatible.class);
        }

        builder.addModifiers(Modifier.PUBLIC);
        builder.addField(apiClassName, "client", Modifier.FINAL, Modifier.PRIVATE);
        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(apiClassName, "client")
                .addCode("this.client = client;")
                .build());

        TypeElement typeElement = elements.getTypeElement(apiClassName.reflectionName());
        if (typeElement != null) {
            JavaElementFactory elementFactory = javaVisitorContext.getElementFactory();
            JavaElementAnnotationMetadataFactory elementAnnotationMetadataFactory = javaVisitorContext.getElementAnnotationMetadataFactory();
            JavaClassElement javaClassElement = elementFactory.newSourceClassElement(typeElement, elementAnnotationMetadataFactory);

            List<MethodElement> methodList = javaClassElement.getMethods();
            Map<String, MethodElement> methodMap = methodList.stream().collect(Collectors.toMap(MethodElement::getSimpleName, me -> me));

            List<ClassElement> classList = javaClassElement.getEnclosedElements(ElementQuery.ALL_INNER_CLASSES);
            Map<String, ClassElement> classMap = classList.stream().collect(Collectors.toMap(ClassElement::getSimpleName, ce -> ce));

            for (MethodElement method : methodList) {
                String methodSimpleName = method.getSimpleName();
                if (methodSimpleName.endsWith("Async") && method.getReturnType().getSimpleName().equals("Call")) {
                    String baseName = methodSimpleName.substring(0, methodSimpleName.lastIndexOf("Async"));

                    String requestClassName = "API" + baseName + "Request";
                    String reactiveRequestClassName = requestClassName + "Reactive";

                    ClassElement requestClass = classMap.get(simpleName + "$" + requestClassName);
                    Optional<TypeSpec> reactiveRequestClassOpt = createRequestClass(reactiveRequestClassName, requestClass, clientType);
                    if (reactiveRequestClassOpt.isPresent()) {
                        MethodElement baseMethod = methodMap.get(baseName);

                        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(baseName)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(ClassName.get(StringUtils.EMPTY_STRING, reactiveRequestClassName));

                        List<String> paramNames = new ArrayList<>();
                        for (ParameterElement parameter : baseMethod.getParameters()) {
                            TypeName paramType = ClassName.get(parameter.getType().getPackageName(), parameter.getType().getSimpleName());
                            methodBuilder.addParameter(paramType, parameter.getSimpleName());
                            paramNames.add(parameter.getSimpleName());
                        }
                        methodBuilder.addCode("  return new " + reactiveRequestClassName + "(client." + baseName + "(" + String.join(", ", paramNames) + "));");

                        builder.addMethod(methodBuilder.build());
                        builder.addType(reactiveRequestClassOpt.get());
                    }
                }
            }
        }

        writeJavaFile(e, reactiveClientClassName, builder.build());
    }

    /**
     * Creates a request class, which contains reactive execute method, from given kubernetes api request class.
     *
     * @param name         the name of the new class
     * @param requestClass the request class from kubernetes api
     * @param clientType   the client type
     * @return the reactive request class
     */
    private Optional<TypeSpec> createRequestClass(String name, ClassElement requestClass, ClientType clientType) {
        String requestClassSimpleName = requestClass.getType().getSimpleName();
        TypeName type = ClassName.get(requestClass.getType().getPackageName(), requestClassSimpleName.replace('$', '.'));

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name);
        classBuilder.addModifiers(Modifier.PUBLIC);
        classBuilder.addField(type, "request", Modifier.FINAL, Modifier.PRIVATE);

        classBuilder.addMethod(MethodSpec.constructorBuilder()
            .addParameter(type, "request")
            .addCode("this.request = request;")
            .build());

        requestClass.getFields().forEach(field -> {
            if (!field.getModifiers().contains(ElementModifier.FINAL)) {
                String fieldName = field.getSimpleName();
                classBuilder.addMethod(MethodSpec.methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(field.getType().getPackageName(), field.getType().getSimpleName()), fieldName)
                    .returns(ClassName.get(StringUtils.EMPTY_STRING, name))
                    .addCode("request." + fieldName + "(" + fieldName + ");\nreturn this;")
                    .build());
            }
        });

        Optional<ClassName> typeArgOpt = findTypeArg(requestClass);
        if (typeArgOpt.isEmpty()) {
            return Optional.empty();
        }

        classBuilder.addMethod(createExecuteMethod(clientType, typeArgOpt.get()));

        return Optional.of(classBuilder.build());
    }

    private Optional<ClassName> findTypeArg(ClassElement requestClass) {
        Optional<MethodElement> executeAsyncMethodOpt = requestClass.getMethods()
            .stream()
            .filter(method -> method.getSimpleName().equals("executeAsync"))
            .findFirst();

        if (executeAsyncMethodOpt.isEmpty()) {
            return Optional.empty();
        }

        MethodElement executeAsyncMethod = executeAsyncMethodOpt.get();
        ParameterElement[] parameters = executeAsyncMethod.getParameters();
        if (parameters.length != 1) {
            return Optional.empty();
        }
        Map<String, ClassElement> typeArguments = parameters[0].getType().getTypeArguments();
        if (typeArguments.size() != 1) {
            return Optional.empty();
        }
        ClassElement typeArgClass = typeArguments.values().stream().findFirst().get();
        return Optional.of(ClassName.get(typeArgClass.getPackageName(), typeArgClass.getSimpleName()));
    }

    private MethodSpec createExecuteMethod(ClientType clientType, ClassName typeArg) {
        ClassName reactiveClassName;
        String methodCode;
        if (clientType == ClientType.REACTOR) {
            reactiveClassName = REACTOR_CLASS_NAME;
            methodCode = REACTOR_METHOD_CODE;
        } else if (clientType == ClientType.RXJAVA2) {
            reactiveClassName = RXJAVA2_CLASS_NAME;
            methodCode = RXJAVA_METHOD_CODE;
        } else {
            reactiveClassName = RXJAVA3_CLASS_NAME;
            methodCode = RXJAVA_METHOD_CODE;
        }
        return MethodSpec.methodBuilder("execute")
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(reactiveClassName, typeArg))
            .addCode(methodCode)
            .build();
    }

    private void writeJavaFile(Element e, ClassName className, TypeSpec typeSpec) {
        final JavaFile javaFile = JavaFile.builder(className.packageName(), typeSpec).build();
        try {
            final JavaFileObject javaFileObject = filer.createSourceFile(className.reflectionName(), e);
            try (Writer writer = javaFileObject.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (IOException ioException) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating '" + className.reflectionName() + "': " + ioException.getMessage(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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

    private ClientType resolveClientType(Element e) {
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
                            return ClientType.valueOf(v.toString());
                        }
                    }
                }
            }
        }
        return ClientType.ASYNC;
    }

    enum ClientType {
        ASYNC, REACTOR, RXJAVA2, RXJAVA3
    }
}
