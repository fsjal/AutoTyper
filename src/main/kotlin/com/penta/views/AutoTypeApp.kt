package com.penta.views

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.TabPane
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx

@ExperimentalCoroutinesApi
class AutoTypeApp : Application(), CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.JavaFx + job
    private val tabs = TabPane()

    override fun start(primaryStage: Stage) {
        val scene = Scene(tabs, 1280.0, 720.0)
        scene.stylesheets.add(javaClass.getResource("/css/java-keywords.css").toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "AutoType"
        primaryStage.show()
        primaryStage.setOnCloseRequest { job.cancel() }
        startTask()
    }

    private fun startTask() {
        launch {
            val files = listOf("/input.java")

            //delay(5000)
            files.forEach {
                val tab = TypeTab(it)
                tabs.tabs.add(tab)
                tabs.selectionModel.select(files.indexOf(it))
                tab.start()
                delay(3000)
            }
        }
    }
}
