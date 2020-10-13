package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.FilesOfAllSubDirectoriesCallable

/**
 * Util object for interaction with publishers.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
object PublisherUtil {

    /**
     * Checks whether the path of the JaCoCo csv file [jacocoCSVPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoCSVPath(workspace: FilePath, jacocoCSVPath: String): Boolean {
        var csvPath = jacocoCSVPath
        if (csvPath.startsWith("**")) csvPath = csvPath.substring(2)
        val split = csvPath.split("/".toRegex())
        val files: List<FilePath>
        files = try {
            workspace.act(
                    FilesOfAllSubDirectoriesCallable(workspace, split[split.size - 1]))
        } catch (ignored: Exception) {
            return false
        }
        for (file in files) {
            if (file.remote.endsWith(csvPath)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks whether the path of the JaCoCo index.html file [jacocoResultsPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoResultsPath(workspace: FilePath, jacocoResultsPath: String): Boolean {
        var resultsPath = jacocoResultsPath
        if (!resultsPath.endsWith("/")) resultsPath += "/"
        if (resultsPath.startsWith("**")) resultsPath = resultsPath.substring(2)
        val files: List<FilePath>
        files = try {
            workspace.act(FilesOfAllSubDirectoriesCallable(workspace, "index.html"))
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        for (file in files) {
            val path = file.remote
            if (path.substring(0, path.length - 10).endsWith(resultsPath)) {
                return true
            }
        }
        return false
    }
}
