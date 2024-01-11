package org.gamekins.util

import org.jsoup.nodes.Document

object JdependUtil {
    fun getCyclesList(document: Document) : Map<String, ArrayList<String>>? {
        val dictionary = mutableMapOf<String, ArrayList<String>>()
        var anchorElements = document.select("a[name=cycles]")
        anchorElements= anchorElements.nextAll().select("table")
        val anchorElement = anchorElements.first()

        var tableElements= anchorElement!!.select("tr")

        if (tableElements.first()!!.select("th").first()!!.text() != "Package")
            return null

        tableElements= tableElements.next()
        for(tableElement in tableElements){
            val rowElements= tableElement.select("td")
            val key= rowElements[0].text()
            var packageDependencies= rowElements[1].text().split("\\s*<br>\\s*")
            packageDependencies= packageDependencies[0].split(" ").toMutableList()

            dictionary[key]= ArrayList(packageDependencies)
        }

        return dictionary
    }
}