package org.gamekins.mutation

import hudson.FilePath
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import jenkins.security.MasterToSlaveCallable
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

class MutationResultsTest : AnnotationSpec() {

    @Test
    fun testCallGetMutationResultsCallable() {
        val remotePath = mockkClass(FilePath::class)
        val uri = URI("abc")
        every { remotePath.toURI() } returns uri
        mockkStatic(Paths::class)
        every { Paths.get(uri) } returns null
        mockkStatic(Files::class)
        every { Files.readAllBytes(any()) } returns ByteArray(1)

        val temp = MutationResults.GetMutationResultsCallable(remotePath)
        temp.call().length shouldBe 1
    }

    @Test
    fun testRetrievedMutationsFromJson() {
        val remotePath = mockkClass(FilePath::class)
        val uri = URI("abc")
        every { remotePath.toURI() } returns uri
        mockkStatic(Paths::class)
        every { Paths.get(uri) } returns null
        mockkStatic(Files::class)
        every { Files.readAllBytes(any()) } returns ByteArray(1)

        val listener = TaskListener.NULL
        every { remotePath.act(any<MasterToSlaveCallable<String, IOException?>>()) } returns ""
        val mutationDetails = MutationDetails(
            mapOf(
                "className" to "org/example/Feature",
                "methodName" to "foo",
                "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
            ),
            listOf(30),
            "ROR",
            "IF_ICMPGT-IF_ICMPLT-50",
            "Hihi.java", 56,
            "replace less than or equal operator with greater than or equal operator",
            listOf("30"), mapOf()
        )
        val mutation = MutationInfo(mutationDetails, "survived", -1547277782)
        val entries = mapOf("org.example.Feature" to listOf(mutation))

        mockkObject(MutationResults.mapper)
        every { MutationResults.mapper.readValue(any<String>(), any<Class<*>>()) } returns MutationResults(entries)

        MutationResults.retrievedMutationsFromJson(remotePath, listener) shouldBe MutationResults(entries)
    }

    @Test
    fun testRetrievedMutationsFromJson1() {
        val remotePath = mockkClass(FilePath::class)
        val listener = TaskListener.NULL
        unmockkAll()
        MutationResults.retrievedMutationsFromJson(remotePath, listener) shouldBe null

    }
}