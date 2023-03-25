package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import java.io.File

internal abstract class TaskSorter(private val project: Project) {
    private val before: TaskCollection<Task> = project.tasks.matching { isBefore(it) }

    private val after: TaskCollection<Task> = project.tasks.matching { isAfter(it) }

    private val beforeByOutput: MutableMap<File, MutableSet<Task>> = mutableMapOf()

    private val afterByInput: MutableMap<File, MutableSet<Task>> = mutableMapOf()

    private val logged: MutableSet<Triple<Task, Task, File>> = mutableSetOf()

    private fun forceOrdering(before: Task, after: Task, file: File) {
        after.mustRunAfter(before)
        val logNotice = Triple(before, after, file)
        if (logNotice !in logged) {
            logged += logNotice
            project.log(
                "make task ${after.path} run after ${before.path}, " +
                    "as file $file is produced by the latter task and consumed by the former one"
            )
        }
    }

    protected abstract fun isBefore(task: Task): Boolean

    protected abstract fun isAfter(task: Task): Boolean

    private fun onBeforeAdd(task: Task, output: File) {
        if (output !in beforeByOutput) {
            beforeByOutput[output] = mutableSetOf()
        }
        beforeByOutput[output]?.add(task)
//        project.log("inspect task ${task.path} producing $output")
        if (output in afterByInput) {
            afterByInput[output]?.forEach {
                forceOrdering(task, it, output)
            }
        }
    }

    private fun onAfterAdd(task: Task, input: File) {
        if (input !in afterByInput) {
            afterByInput[input] = mutableSetOf()
        }
        afterByInput[input]?.add(task)
//        project.log("inspect task ${task.path} consuming $input")
        if (input in beforeByOutput) {
            beforeByOutput[input]?.forEach {
                forceOrdering(it, task, input)
            }
        }
    }

    fun enforceOrdering() {
        before.all { beforeTask ->
            beforeTask.outputs.files.files.forEach { outputFile ->
                onBeforeAdd(beforeTask, outputFile)
            }
        }
        after.all { afterTask ->
            afterTask.inputs.files.files.forEach { inputFile ->
                onAfterAdd(afterTask, inputFile)
            }
        }
    }

    companion object {
        fun Project.enforceOrderingAmongTasks(
            potentiallyBefore: (Task) -> Boolean,
            potentiallyAfter: (Task) -> Boolean
        ) {
            val enforcer = object : TaskSorter(this@enforceOrderingAmongTasks) {
                override fun isBefore(task: Task): Boolean = potentiallyBefore(task)
                override fun isAfter(task: Task): Boolean = potentiallyAfter(task)
            }
            enforcer.enforceOrdering()
        }
    }
}
