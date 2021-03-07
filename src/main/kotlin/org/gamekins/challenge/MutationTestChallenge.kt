/*
 * Copyright 2020 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JacocoUtil.ClassDetails


/**
 * Specific [Challenge] to motivate users to write test cases to kill a generated mutant.
 *
 * @author Tran Phan
 * @since 1.0
 */
class MutationTestChallenge(
    val mutationInfo: MutationInfo, val classDetails: ClassDetails,
    val branch: String?, workspace: FilePath, val commitID: String
) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private var mutationDetails = mutationInfo.mutationDetails
    private val methodName = mutationDetails.methodInfo["methodName"]
    private val className = mutationDetails.methodInfo["className"]?.replace("/", ".")
    private val mutationDescription = mutationDetails.mutationDescription
    private val lineOfCode = mutationDetails.loc
    private val fileName = mutationDetails.fileName
    val uniqueID = mutationInfo.uniqueID
    private val codeSnippet = createCodeSnippet(classDetails, lineOfCode, workspace)

    override fun getCreated(): Long {
        return created
    }

    fun getName(): String {
        return "MutationTestChallenge"
    }

    fun getMutationDescription(): String {
        return mutationDescription
    }

    fun getFileName(): String {
        return if (codeSnippet.isNotEmpty()) fileName else ""
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + getName()
                + " created=\"" + created
                + "\" solved=\"" + solved
                + "\" class=\"" + className
                + "\" method=\"" + methodName
                + "\" lineOfCode=\"" + lineOfCode
                + "\" mutationDescription=\"" + mutationDescription
                + "\" result=\"" + mutationInfo.result)

        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    /**
     * Needed because of automatically generated getter and setter in Kotlin.
     */
    fun setSolved(newSolved: Long) {
        solved = newSolved
    }


    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MutationTestChallenge) return false
        return other.mutationInfo == this.mutationInfo
    }

    /**
     * Returns the constants provided during creation. Must include entries for "projectName", "branch", "workspace",
     * "jacocoResultsPath" and "jacocoCSVPath".
     */
    override fun getConstants(): HashMap<String, String> {
        return classDetails.constants
    }


    override fun getScore(): Int {
        //fixme: find a way to determine mutation score - could be based on complexity compared to original code
        return 4
    }

    override fun hashCode(): Int {
        return this.mutationInfo.hashCode()
    }

    /**
     * Checks whether the [MutationTestChallenge] is solvable if the [run] was in the [branch] (taken from
     * [constants]), where it has been generated. The [workspace] is the folder with the code and execution rights,
     * and the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolvable(
        constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
        workspace: FilePath
    ): Boolean {
        // a mutation challenge is discarded if the source file has changed since last commit
        val changedClasses =  workspace.act(GitUtil.DiffFromHeadCallable(workspace, commitID,
                                                classDetails.packageName, listener))
        return changedClasses?.any { it.replace("/", ".") ==
                                     "${classDetails.packageName}.${classDetails.className}" } == false
    }

    /**
     * The [MutationTestChallenge] is solved if mutation status is killed.
     * The [workspace] is the folder with the code and execution rights, and
     * the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(
        constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
        workspace: FilePath
    ): Boolean {
        val jsonFilePath = JacocoUtil.calculateCurrentFilePath(
            workspace, classDetails.mocoJSONFile, classDetails.workspace
        )
        val mutationResults = MutationResults.retrievedMutationsFromJson(jsonFilePath, listener)
        val filteredByClass = mutationResults?.entries?.filter { it.key == this.className }
        if (!filteredByClass.isNullOrEmpty()) {
            return filteredByClass.any {
                it.value.any { it1 ->
                    ((it1.uniqueID == uniqueID || it1.mutationDetails == mutationDetails)
                            && it1.result == "killed")
                }
            }
        }
        return false
    }

    override fun toString(): String {
        return ("Write a test to kill the mutant \"<b>$mutationDescription</b>\" at line <b>$lineOfCode</b> " +
                "of method <b>$methodName</b> in class <b>${classDetails.className}</b> in package " +
                "<b>${classDetails.packageName}</b> (created for branch " + branch + ")")
    }

    fun getSnippet(): String {
        return codeSnippet
    }

    fun createCodeSnippet(classDetails: ClassDetails, lineOfCode: Int, workspace: FilePath): String {
        if (lineOfCode < 0) {
            return ""
        }
        if (classDetails.jacocoSourceFile.exists()) {
            val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                workspace, classDetails.jacocoSourceFile, classDetails.workspace
            )
            val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, lineOfCode, 4)
            if (snippetElements == "") {
                return ""
            }
            return "<pre class='prettyprint linenums:${lineOfCode - 2} mt-2'><code class='language-java'>" + snippetElements +
                    "</code></pre>"
        }
        return ""
    }
}
