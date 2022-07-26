package com.jhvictor4.swaggerit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.jhvictor4.swaggerit.BASE_PACKAGE_NAME
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.converter.ResolvedSchema
import io.swagger.v3.oas.models.media.Schema
import org.reflections.Reflections
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@Configuration
class SwaggerConfig(
    private val modelConverter: CustomAdditionalModelsConverter,
) {
    @Bean
    fun openApi(openApiCustomizer: OpenApiCustomiser): GroupedOpenApi {
        return GroupedOpenApi
            .builder()
            .packagesToScan(BASE_PACKAGE_NAME)
            .group("")
            .addOpenApiCustomiser(openApiCustomizer)
            .build()
    }

    @Bean
    fun openApiCustomizer() = OpenApiCustomiser { openApi ->
        val openApiSchemas = openApi.components.schemas
        val localCache = modelConverter.schemaLocalCache

        // add additionally scanned subType schemas to OpenApi Schema List
        val resolvedSubTypeSchemas = localCache.values.flatten()
        resolvedSubTypeSchemas.forEach { resolvedSchema ->
            openApiSchemas.putIfAbsent(resolvedSchema.schema.name, resolvedSchema.schema)
            resolvedSchema.referencedSchemas.forEach(openApiSchemas::put)
        }

        // add reference schemas to interface schemas
        localCache.forEach { (interfaceSchemaName, resolvedSubTypeSchemas) ->
            val childSchemas = resolvedSubTypeSchemas.map { resolvedSchema -> Schema<Any>().apply { `$ref` = resolvedSchema.schema.name } }
            val navigatingSchema = Schema<Any>().anyOf(childSchemas)
            openApiSchemas[interfaceSchemaName] = navigatingSchema
        }

        localCache.clear()
    }
}

@Component
class CustomAdditionalModelsConverter(
    val objectMapper: ObjectMapper,
) : ModelConverter {

    val schemaLocalCache = mutableMapOf<String, MutableSet<ResolvedSchema>>()
    override fun resolve(
        type: AnnotatedType,
        context: ModelConverterContext?,
        chain: Iterator<ModelConverter>,
    ): Schema<*>? {
        val resultSchema = if (chain.hasNext()) chain.next().resolve(type, context, chain) else null
        return resultSchema?.also {
            val kClass = objectMapper.constructType(type.type).rawClass.kotlin
            saveSubTypesToLocalCacheIfIsInterface(kClass)
        }
    }

    private fun saveSubTypesToLocalCacheIfIsInterface(kotlinType: KClass<*>) {
        if (
            !kotlinType.isMyAbstract ||
            schemaLocalCache.containsKey(kotlinType.simpleName)
        ) return

        schemaLocalCache[kotlinType.simpleName!!] = mutableSetOf()

        // insert all subType schemas into local cache
        fetchAllSubTypes(kotlinType).forEach { subClass ->
            val resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(subClass.java)
            schemaLocalCache[kotlinType.simpleName!!]!!.add(resolvedSchema)
        }

        // check For member properties
        kotlinType.memberProperties
            .mapNotNull { property -> property.returnType.classifier as? KClass<*> }
            .filter { kClass -> kClass.isMyAbstract }
            .forEach { kClass -> saveSubTypesToLocalCacheIfIsInterface(kClass) }
    }

    private fun fetchAllSubTypes(kClass: KClass<*>): Set<KClass<*>> {
        return Reflections(BASE_PACKAGE_NAME).getSubTypesOf(kClass.java).map { jvmType -> jvmType.kotlin }.toSet()
    }

    private val KClass<*>.isMyAbstract: Boolean get() {
        val isAbstract = isAbstract || isSealed || java.isInterface
        val isInOurSourceCode = qualifiedName!!.startsWith(BASE_PACKAGE_NAME)
        return isAbstract && isInOurSourceCode
    }
}
