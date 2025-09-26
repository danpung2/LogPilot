package com.logpilot.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionalAnnotationsTest {

    @Test
    void conditionalOnGrpcProtocol_ShouldHaveCorrectAnnotations() {
        Class<ConditionalOnGrpcProtocol> annotationClass = ConditionalOnGrpcProtocol.class;

        assertTrue(annotationClass.isAnnotation());
        assertTrue(annotationClass.isAnnotationPresent(Target.class));
        assertTrue(annotationClass.isAnnotationPresent(Retention.class));
        assertTrue(annotationClass.isAnnotationPresent(ConditionalOnProperty.class));

        Target target = annotationClass.getAnnotation(Target.class);
        ElementType[] targetTypes = target.value();
        assertEquals(2, targetTypes.length);
        assertTrue(java.util.Arrays.asList(targetTypes).contains(ElementType.TYPE));
        assertTrue(java.util.Arrays.asList(targetTypes).contains(ElementType.METHOD));

        Retention retention = annotationClass.getAnnotation(Retention.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());

        ConditionalOnProperty conditionalOnProperty = annotationClass.getAnnotation(ConditionalOnProperty.class);
        String[] values = conditionalOnProperty.value();
        assertEquals(1, values.length);
        assertEquals("logpilot.server.protocol", values[0]);
        assertEquals("grpc", conditionalOnProperty.havingValue());
    }

    @Test
    void conditionalOnRestProtocol_ShouldHaveCorrectAnnotations() {
        Class<ConditionalOnRestProtocol> annotationClass = ConditionalOnRestProtocol.class;

        assertTrue(annotationClass.isAnnotation());
        assertTrue(annotationClass.isAnnotationPresent(Target.class));
        assertTrue(annotationClass.isAnnotationPresent(Retention.class));
        assertTrue(annotationClass.isAnnotationPresent(ConditionalOnProperty.class));

        Target target = annotationClass.getAnnotation(Target.class);
        ElementType[] targetTypes = target.value();
        assertEquals(2, targetTypes.length);
        assertTrue(java.util.Arrays.asList(targetTypes).contains(ElementType.TYPE));
        assertTrue(java.util.Arrays.asList(targetTypes).contains(ElementType.METHOD));

        Retention retention = annotationClass.getAnnotation(Retention.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());

        ConditionalOnProperty conditionalOnProperty = annotationClass.getAnnotation(ConditionalOnProperty.class);
        String[] values = conditionalOnProperty.value();
        assertEquals(1, values.length);
        assertEquals("logpilot.server.protocol", values[0]);
        assertEquals("rest", conditionalOnProperty.havingValue());
    }

    @Test
    void conditionalAnnotations_ShouldHaveDifferentHavingValues() {
        ConditionalOnProperty grpcConditional = ConditionalOnGrpcProtocol.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty restConditional = ConditionalOnRestProtocol.class.getAnnotation(ConditionalOnProperty.class);

        assertNotEquals(grpcConditional.havingValue(), restConditional.havingValue());
        assertEquals("grpc", grpcConditional.havingValue());
        assertEquals("rest", restConditional.havingValue());
        assertArrayEquals(grpcConditional.value(), restConditional.value());
        assertEquals("logpilot.server.protocol", grpcConditional.value()[0]);
    }

    @Test
    void conditionalAnnotations_ShouldTargetSameElements() {
        Target grpcTarget = ConditionalOnGrpcProtocol.class.getAnnotation(Target.class);
        Target restTarget = ConditionalOnRestProtocol.class.getAnnotation(Target.class);

        assertArrayEquals(grpcTarget.value(), restTarget.value());

        ElementType[] expectedTargets = {ElementType.TYPE, ElementType.METHOD};
        assertArrayEquals(expectedTargets, grpcTarget.value());
        assertArrayEquals(expectedTargets, restTarget.value());
    }

    @Test
    void conditionalAnnotations_ShouldHaveRuntimeRetention() {
        Retention grpcRetention = ConditionalOnGrpcProtocol.class.getAnnotation(Retention.class);
        Retention restRetention = ConditionalOnRestProtocol.class.getAnnotation(Retention.class);

        assertEquals(RetentionPolicy.RUNTIME, grpcRetention.value());
        assertEquals(RetentionPolicy.RUNTIME, restRetention.value());
        assertEquals(grpcRetention.value(), restRetention.value());
    }

    @Test
    void conditionalAnnotations_ShouldBeMetaAnnotatedWithConditionalOnProperty() {
        assertTrue(ConditionalOnGrpcProtocol.class.isAnnotationPresent(ConditionalOnProperty.class));
        assertTrue(ConditionalOnRestProtocol.class.isAnnotationPresent(ConditionalOnProperty.class));

        ConditionalOnProperty grpcProperty = ConditionalOnGrpcProtocol.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty restProperty = ConditionalOnRestProtocol.class.getAnnotation(ConditionalOnProperty.class);

        assertEquals("logpilot.server.protocol", grpcProperty.value()[0]);
        assertEquals("logpilot.server.protocol", restProperty.value()[0]);

        assertEquals("grpc", grpcProperty.havingValue());
        assertEquals("rest", restProperty.havingValue());
    }

    @ConditionalOnGrpcProtocol
    static class TestGrpcClass {
    }

    @ConditionalOnRestProtocol
    static class TestRestClass {
    }

    @Test
    void conditionalAnnotations_ShouldBeApplicableToClasses() {
        assertTrue(TestGrpcClass.class.isAnnotationPresent(ConditionalOnGrpcProtocol.class));
        assertTrue(TestRestClass.class.isAnnotationPresent(ConditionalOnRestProtocol.class));

        ConditionalOnGrpcProtocol grpcAnnotation = TestGrpcClass.class.getAnnotation(ConditionalOnGrpcProtocol.class);
        ConditionalOnRestProtocol restAnnotation = TestRestClass.class.getAnnotation(ConditionalOnRestProtocol.class);

        assertNotNull(grpcAnnotation);
        assertNotNull(restAnnotation);
    }

    static class TestMethodClass {
        @ConditionalOnGrpcProtocol
        public void grpcMethod() {
        }

        @ConditionalOnRestProtocol
        public void restMethod() {
        }
    }

    @Test
    void conditionalAnnotations_ShouldBeApplicableToMethods() throws NoSuchMethodException {
        java.lang.reflect.Method grpcMethod = TestMethodClass.class.getMethod("grpcMethod");
        java.lang.reflect.Method restMethod = TestMethodClass.class.getMethod("restMethod");

        assertTrue(grpcMethod.isAnnotationPresent(ConditionalOnGrpcProtocol.class));
        assertTrue(restMethod.isAnnotationPresent(ConditionalOnRestProtocol.class));

        ConditionalOnGrpcProtocol grpcAnnotation = grpcMethod.getAnnotation(ConditionalOnGrpcProtocol.class);
        ConditionalOnRestProtocol restAnnotation = restMethod.getAnnotation(ConditionalOnRestProtocol.class);

        assertNotNull(grpcAnnotation);
        assertNotNull(restAnnotation);
    }
}