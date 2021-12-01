package com.github.soarex16.doccommentrepl.python

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.EncodingEnvironmentUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.python.PyBundle
import com.jetbrains.python.console.*
import com.jetbrains.python.console.PyConsoleOptions.PyConsoleSettings
import com.jetbrains.python.console.PydevConsoleRunner
import com.jetbrains.python.console.PydevConsoleRunnerImpl.PythonConsoleRunParams
import com.jetbrains.python.remote.PyRemoteSdkAdditionalDataBase
import com.jetbrains.python.run.PythonCommandLineState


class PydevConsoleRunner(val project: Project, val sdk: Sdk?, val consoleType: PyConsoleType,
                         val title: String?, val workingDir: String?, val environmentVariables: Map<String, String>,
                         val settingsProvider: PyConsoleSettings) {

    lateinit var remoteConsoleProcessData: RemoteConsoleProcessData
    lateinit var pydevConsoleCommunication: PydevConsoleCommunication

    fun getRemoteAdditionalData(sdk: Sdk?): PyRemoteSdkAdditionalDataBase? {
        val sdkAdditionalData = sdk?.sdkAdditionalData ?: return null
        return if (sdkAdditionalData is PyRemoteSdkAdditionalDataBase) {
            sdkAdditionalData
        } else {
            null
        }
    }

    fun createConsoleRunParams(workingDir: String?,
                                         sdk: Sdk,
                                         environmentVariables: Map<String, String>): PythonConsoleRunParams {
        return PythonConsoleRunParams(settingsProvider, workingDir, sdk, environmentVariables)
    }

    private fun createCommandLine(sdk: Sdk,
                                       environmentVariables: Map<String, String>,
                                       workingDir: String?, port: Int): GeneralCommandLine {
        val runParams: PythonConsoleRunParams = createConsoleRunParams(workingDir, sdk, environmentVariables)
        val title = PyBundle.message("connecting.to.console.title")

        val cmd = ProgressManager.getInstance().run(object : Task.WithResult<GeneralCommandLine?, RuntimeException?>(project, title, false) {
            override fun compute(indicator: ProgressIndicator): GeneralCommandLine {
                return PythonCommandLineState.createPythonCommandLine(myProject, sdk.sdkAdditionalData, runParams, false,
                        PtyCommandLine.isEnabled() && !SystemInfo.isWindows)
            }
        })!!

        cmd.withWorkDirectory(workingDir)
        val exeGroup = cmd.parametersList.getParamsGroup(PythonCommandLineState.GROUP_EXE_OPTIONS)
        if (exeGroup != null && !runParams.interpreterOptions.isEmpty()) {
            exeGroup.addParametersString(runParams.interpreterOptions)
        }
        cmd.setupPythonConsoleScriptInClientMode(sdk, port)
        return cmd
    }

    fun getRunnerFileFromHelpers() = PYDEV_PYDEVCONSOLE_PY

    @Throws(ExecutionException::class)
    private fun createProcess(sdk: Sdk): CommandLineProcess {
        val remoteSdkAdditionalData = getRemoteAdditionalData(sdk)
        return if (remoteSdkAdditionalData != null) {
            val generalCommandLine: GeneralCommandLine = createCommandLine(sdk, environmentVariables, workingDir, 0)
            val pathMapper = PydevConsoleRunner.getPathMapper(project, settingsProvider, remoteSdkAdditionalData)
            val remoteConsoleProcessData = createRemoteConsoleProcess(generalCommandLine,
                    pathMapper,
                    project, remoteSdkAdditionalData, getRunnerFileFromHelpers())

            this.remoteConsoleProcessData = remoteConsoleProcessData
            this.pydevConsoleCommunication = remoteConsoleProcessData.pydevConsoleCommunication
            CommandLineProcess(remoteConsoleProcessData.process, remoteConsoleProcessData.commandLine)
        } else {
            val port = PydevConsoleRunnerImpl.findAvailablePort(project, consoleType)
            val generalCommandLine: GeneralCommandLine = createCommandLine(sdk, environmentVariables, workingDir, port)
            val envs = generalCommandLine.environment
            EncodingEnvironmentUtil.setLocaleEnvironmentIfMac(envs, generalCommandLine.charset)
            val communicationServer = PydevConsoleCommunicationServer(project, LOCALHOST, port)
            this.pydevConsoleCommunication = communicationServer
            try {
                communicationServer.serve()
            } catch (e: Exception) {
                communicationServer.close()
                throw ExecutionException(e.message, e)
            }
            val process = generalCommandLine.createProcess()
            communicationServer.setPythonConsoleProcess(process)
            CommandLineProcess(process, generalCommandLine.commandLineString)
        }
    }

    companion object {
        private val LOCALHOST: String = "localhost"

        val WORKING_DIR_AND_PYTHON_PATHS: String = "WORKING_DIR_AND_PYTHON_PATHS"
        val CONSOLE_START_COMMAND: String = """
            import sys; print('Python %s on %s' % (sys.version, sys.platform))
            sys.path.extend([$WORKING_DIR_AND_PYTHON_PATHS])
            
            """.trimIndent()
        val STARTED_BY_RUNNER: String = "startedByRunner"
        val INLINE_OUTPUT_SUPPORTED: String = "INLINE_OUTPUT_SUPPORTED"
        private const val WAIT_BEFORE_FORCED_CLOSE_MILLIS = 2000L
        private val LOG = Logger.getInstance(PydevConsoleRunnerImpl::class.java)
        val PYDEV_PYDEVCONSOLE_PY: String = "pydev/pydevconsole.py"
        const val PORTS_WAITING_TIMEOUT = 20000
    }
}