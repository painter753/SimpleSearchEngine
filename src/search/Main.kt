package search

import java.io.File

const val MSG_GET_STRATEGY = "Select a matching strategy: ALL, ANY, NONE, BACK to return to main menu"
const val MSG_GET_QUERY = "Enter data to search people:"
const val MSG_GET_RESULT = "People found:"
const val MSG_NOT_FOUND = "No matching people found."

fun main(args: Array<String>) {
    val optionDataIndex = args.indexOf("--data")
    val source = args[optionDataIndex + 1]

    SearchEngine().initialize(source).start()
}

class SearchEngine {

    enum class Strategy {
        ANY,
        ALL,
        NONE,
        BACK
    }

    private var dataset = emptyList<String>()
    private var invertedIndex = emptyMap<String, List<Int>>()

    fun initialize(source: String): SearchEngine {
        val file = File(source)
        dataset = createDataset(file)
        invertedIndex = createInvertedIndex(file)
        return this
    }

    fun start() {
        val menu = Menu()
        while (true) {
            when (menu.chooseOption()) {
                Menu.Action.PRINT_ALL -> printAll()
                Menu.Action.FIND -> find()
                Menu.Action.EXIT -> break
            }
        }
        println("Bye!")
    }

    private fun printAll() {
        dataset.forEach { println(it) }
    }

    private fun find() {
        println(MSG_GET_STRATEGY)
        val strategy = Strategy.valueOf(readLine()!!.uppercase())
        if (strategy == Strategy.BACK) return

        println(MSG_GET_QUERY)
        val words = readLine()!!.lowercase().split(" ")
        var indexes = when (strategy) {
            Strategy.ALL -> findUsingAllStrategy(words)
            Strategy.ANY -> findUsingAnyStrategy(words)
            Strategy.NONE -> findUsingNoneStrategy(words)
            Strategy.BACK -> return
        }
        printResult(indexes.map { dataset[it] }.toMutableList())
    }

    private fun findUsingAllStrategy(words: List<String>): MutableList<Int> = words.fold(mutableListOf()) { acc, s ->
        if (acc.isEmpty())
            acc.union(invertedIndex.getOrDefault(s.lowercase(), mutableListOf())).toMutableList()
        else
            acc.intersect(invertedIndex.getOrDefault(s.lowercase(), mutableListOf())).toMutableList() }
    private fun findUsingAnyStrategy(words: List<String>): MutableList<Int> = words.fold(mutableListOf()) { acc, s ->
        acc.union(invertedIndex.getOrDefault(s.lowercase(), mutableListOf())).toMutableList() }
    private fun findUsingNoneStrategy(words: List<String>): MutableList<Int> {
        var indexes = words.fold(mutableListOf<Int>()) { acc, s ->
            acc.union(invertedIndex.getOrDefault(s.lowercase(), mutableListOf())).toMutableList() }
        return invertedIndex.map { it.value }.reduce { f, s ->
            f.union(s).toMutableList() }.subtract(indexes).toMutableList()
    }

    private fun createDataset(file: File): List<String> {
        return if (file.exists()) {
            file.readLines(Charsets.UTF_8)
        } else {
            emptyList()
        }
    }
    private fun createInvertedIndex(file: File): Map<String, List<Int>> {
        return if (file.exists()) {
            val map = mutableMapOf<String, List<Int>>()
            val lines = file.readLines(Charsets.UTF_8)
            for (lineIndex in lines.indices) {
                lines[lineIndex].lowercase().split(" ")
                    .forEach { map.merge(it, mutableListOf(lineIndex)) { f, l -> f + l } }
            }
            map
        } else {
            emptyMap()
        }
    }

    private fun printResult(list: List<String>) = if (list.isEmpty()) println(MSG_NOT_FOUND) else println(MSG_GET_RESULT).also { list.forEach { println(it) } }
}

class Menu {

    private val validator = InputValidator()
    private val menuList = listOf(
        MenuOption(1, Action.FIND, "Find a person"),
        MenuOption(2, Action.PRINT_ALL,"Print all people"),
        MenuOption(0, Action.EXIT,"Exit")
    )

    fun chooseOption(): Action {
        while (true) {
            menuList.forEach { println("${it.index}. ${it.description}") }
            val input = readLine()!!

            if (validator.isValid(input)) {
                return menuList.first { it.index == input.toInt() }.command
            } else {
                println("Incorrect option! Try again.")
            }
        }
    }

    enum class Action {
        FIND,
        PRINT_ALL,
        EXIT,
    }

    private inner class InputValidator {
        fun isValid(input: String) =  input.all { it.isDigit() } && menuList.any { input.toInt() == it.index }
    }

    private class MenuOption(val index: Int, val command: Action, val description: String)

}

s