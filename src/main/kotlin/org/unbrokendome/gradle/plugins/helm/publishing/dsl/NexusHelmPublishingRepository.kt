package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.SerializableCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.toSerializable
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.PublisherParams
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.toMultipartBody
import java.io.File
import java.net.URI
import javax.inject.Inject


interface NexusHelmPublishingRepository : HelmPublishingRepository {
    /**
     * Helm repository name.
     *
     * Defaults empty string.
     */
    val repository: Property<String>

    /**
     * Version of nexus API.
     *
     * Defaults to `v1`.
     */
    val apiVersion: Property<String>
}


private open class DefaultNexusHelmPublishingRepository @Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), NexusHelmPublishingRepository {

    override val repository: Property<String> =
        objects.property<String>()
            .convention("")


    override val apiVersion: Property<String> =
        objects.property<String>()
            .convention("v1")

    override val publisherParams: PublisherParams
        get() = NexusPublisherParams(
            url = url.get(),
            credentials = configuredCredentials.orNull?.toSerializable(),
            repository = repository.get(),
            apiVersion = apiVersion.get()
        )


    private class NexusPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?,
        private val repository: String,
        private val apiVersion: String
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            NexusPublisher(url, credentials, repository, apiVersion)
    }


    private class NexusPublisher(
        url: URI,
        credentials: SerializableCredentials?,
        private val repository: String,
        private val apiVersion: String
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "POST"

        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/service/rest/$apiVersion/components${
                repository.takeIf { it.isNotEmpty() }?.let { "?repository=$it" }.orEmpty()
            }"

        override fun requestBody(chartFile: File) = super.requestBody(chartFile).toMultipartBody(chartFile.name)
    }
}


internal fun ObjectFactory.newNexusHelmPublishingRepository(name: String): NexusHelmPublishingRepository =
    newInstance(DefaultNexusHelmPublishingRepository::class.java, name)
