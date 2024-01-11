package org.gamekins.challenge

import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JdependUtil.getCyclesList
import org.jsoup.nodes.Document
import java.io.File

class CyclesJDChallenge(private val data: Challenge.ChallengeGenerationData,
                        private val cyclesDictionary: Map<String, ArrayList<String>>)
    : Challenge{
    private val created= System.currentTimeMillis()
    private var solved: Long= 0
    private var chosenPackage: String=""
    private var cyclePackage: String=""
    init {
        val selectedKeys= mutableSetOf<String>()

        while(cyclePackage == ""){
            val availableKeys = cyclesDictionary.keys-selectedKeys
            chosenPackage= availableKeys.random()
            selectedKeys.add(chosenPackage)

            val shufflePackageDependencies= cyclesDictionary[chosenPackage]!!.shuffled()
            for(packageDependency in shufflePackageDependencies){
                if (cyclePackage != "")
                    break

                if(chosenPackage == packageDependency)
                    continue

                for (innerPackageDependency in cyclesDictionary[packageDependency]!!)
                    if (innerPackageDependency == chosenPackage) {
                        cyclePackage = packageDependency    //Trova il ciclo
                        break
                    }
            }
        }
    }
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is CyclesJDChallenge) return false

        return (this.cyclePackage == other.cyclePackage && this.chosenPackage == other.chosenPackage) ||
                (this.cyclePackage == other.chosenPackage && this.chosenPackage == other.cyclePackage)
    }

    override fun getParameters(): Constants.Parameters {
        return data.parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return "Dependencies Cycle"
    }

    override fun getScore(): Int {
        return 5
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        return checkPackageExist(parameters)
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (!checkPackageExist(parameters))
            return false

        val document= generateJDDocument(parameters, listener) ?: return false
        val newCycleList= getCyclesList(document) ?: return true

        if (!newCycleList.containsKey(cyclePackage) || !newCycleList.containsKey(chosenPackage))
            return true

        if (newCycleList[chosenPackage]?.contains(cyclePackage) == true &&
            newCycleList[cyclePackage]?.contains(chosenPackage) == true)
            return false

        solved= System.currentTimeMillis()
        return true
    }

    override fun printToXML(reason: String, indentation: String): String? {
        var print = (indentation + "<" + this::class.simpleName + " created=\"" + created + "\" solved=\"" + solved
                 + "\" chosen package=\"" + chosenPackage + "\" cycle package=\"" + cyclePackage )
        if (reason.isNotEmpty())
            print += "\" reason=\"$reason"

        print += "\"/>"
        return print
    }

    override fun toString(): String {
        return ("Solve the cycle between packages <b>${chosenPackage}</b> and <b>${cyclePackage}</b>")
    }

    private fun findFolderInTree(rootDirectory: File, targetFolderName: String): File? {
        // Verifica se il rootDirectory esiste ed è una directory
        if (!rootDirectory.exists() || !rootDirectory.isDirectory)
            throw IllegalArgumentException("La radice deve essere una directory esistente.")

        // Funzione ricorsiva per cercare la cartella nel tree
        fun searchFolder(currentDirectory: File): File? {
            val contents = currentDirectory.listFiles() ?: return null

            for (item in contents)
                if (item.isDirectory) {
                    if (item.name == targetFolderName)
                        return item  // Trovata la cartella con il nome desiderato
                    else {
                        val foundInSubtree = searchFolder(item)
                        if (foundInSubtree != null)
                            return foundInSubtree  // Trovata nel sottoalbero
                    }
                }

            return null
        }

        return searchFolder(rootDirectory)
    }

    private fun checkPackageExist(parameters: Constants.Parameters): Boolean{
        val lastFolderChosen= chosenPackage.split(".").last()
        if (findFolderInTree(File(parameters.remote), lastFolderChosen) == null)
            return false

        val lastFolderCycle= cyclePackage.split(".").last()
        return findFolderInTree(File(parameters.remote), lastFolderCycle) != null
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + cyclesDictionary.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        result = 31 * result + chosenPackage.hashCode()
        result = 31 * result + cyclePackage.hashCode()
        return result
    }

    private fun generateJDDocument(parameters: Constants.Parameters, listener: TaskListener): Document?{
        val jdependHTMLPath= StringBuilder(parameters.remote)
        val jdependHTMLFile= File(jdependHTMLPath.toString()+parameters.jdependResultsPath.substring(2))
        val jdependSourceFile= JacocoUtil.calculateCurrentFilePath(parameters.workspace, jdependHTMLFile)

        val document: Document = try {
            if (!jdependSourceFile.exists())
                return null
            JacocoUtil.generateDocument(jdependSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return null
        }
        return document
    }
}