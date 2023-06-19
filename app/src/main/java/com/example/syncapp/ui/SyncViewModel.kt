package com.example.syncapp.ui

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.syncapp.context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.Executors


var socketConnectionTag = "SocketConnectionTag"
var green = Color(0xFF00e3ab)
val red = Color(0xFFFF1440)

@RequiresApi(Build.VERSION_CODES.O)
class SyncViewModel: ViewModel()
{
    private val _uiState = MutableStateFlow(SyncUiState())

    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    var connectionColor by mutableStateOf(red)


    fun updateConnection()
    {
        connectionColor = green
        _uiState.update { SyncUiState(connectionColor = connectionColor, isConnected = isConnected) }
    }

    //  variable for the  socket connection object
    var socket: Socket? = null

    //  notifies system that socket is currently receiving data
    var isRequesting by mutableStateOf(false)

    //  notifies system that socket is connected to a client
    var isConnected by mutableStateOf(false)

    //  String: stores stage status, eg.. failed to connect, connected, syncing, sync complete etc.
    var statusString by mutableStateOf("---")

    //  channels for writing and reading
    var socketReader: DataInputStream? = null
    var socketWriter: DataOutputStream? = null

    //  stores the missing files need for sync
    var missingFiles = mutableListOf<String>()

    var localFilesCount by mutableStateOf(0)
    var localDirsCount by mutableStateOf(0)
    var localSize by mutableStateOf(0)
    var globalFilesCount by mutableStateOf(0)
    var globalDirsCount by mutableStateOf(0)
    var globalSize by mutableStateOf(0)

    var syncDate by mutableStateOf("---")

    //  comparison, if contents of local are equal to global then system is up-to-date
    var isUpToDate by mutableStateOf(false)

    //  creates the write and read channels
    private fun setDescriptors()
    {
        socketReader = DataInputStream(socket?.getInputStream())
        socketWriter = DataOutputStream(socket?.getOutputStream())
    }

    init {
        connectionColor = red
        getLocalAttributes()
        loadData()
        //  loads ui values for date, and global attributes
        initialUiUpdate()
    }

    //  function connects this client to the server
    //  ip and port are currently hardwired but future works will make it so the user can change
    fun connectToServer()
    {
        try {
            statusString = "Awaiting Server."
            updateStatusString()

            //val add = InetSocketAddress(InetAddress.getByName("fe80::d17a:86be:2da9:f710%12"), 24300)
            socket = Socket()
            socket?.connect(InetSocketAddress("192.168.0.105", 24300), 5000)
            isConnected = true
            setDescriptors()

            //  register a username, in this case i used device type as the username
            socketWriter?.write("phone".toByteArray())

            statusString = "Connected to 192.168.0.105:24300"

            //  updates user components upon connection establishment
            updateConnection()
        } catch (e: Exception)
        {
            Log.d(socketConnectionTag, e.toString())
            statusString = "Failed to Connect."
        }
        updateStatusString()

    }

    //  ui handle that monitors system processes, ergo controlling the progress bar
    var isWorking by mutableStateOf(false)

    //  tells the status of the system, e.g "pulling", "syncing", or name of file being recieved
    fun updateStatusString()
    {
        _uiState.update { SyncUiState(statusString = statusString, isWorking = isWorking) }
    }

    //  updates the ui, concerning the attributes of the global state, incl. size, file count
    //  upon receiving a pull response
    fun updateGlobalAttributes()
    {
        _uiState.update { SyncUiState(globalFilesCount=globalFilesCount, globalDirsCount = globalDirsCount, globalSize = globalSize,
            isUpToDate = isUpToDate) }
    }

    //  checks if system is up-to-date or requires synchronisation
    fun getUpToDate()
    {
        isUpToDate = localFilesCount == globalFilesCount
    }

    //  send pull request
    @RequiresApi(Build.VERSION_CODES.O)
    fun makePullRequest()
    {
        isWorking = true
        statusString = "Initiating Pull"
        updateStatusString()
        if(!isRequesting)
        {
            socketWriter?.write("pull".toByteArray())
            receivePullResponse()
            getLocalFiles()
        }
        getUpToDate()
        updateGlobalAttributes()
        Log.d("Return", "Pull request has returned.")
        isWorking = false
        statusString = "Pull Complete"
        updateStatusString()
    }

    //  parses the system date and time into a string, which is used as the last sync date
    fun getDateTime()
    {
        var dateTime = LocalDateTime.now()
        var day = dateTime.dayOfWeek
        var hour = if(dateTime.hour<10) "0${dateTime.hour}" else "${dateTime.hour}"
        var minute = if(dateTime.minute<10) "0${dateTime.minute}" else "${dateTime.minute}"
        var second = if(dateTime.second<10) "0${dateTime.second}" else "${dateTime.second}"
        var year = dateTime.year
        var month = dateTime.month
        var dayOfMon = dateTime.dayOfMonth
        syncDate = "${hour}:${minute}:${second}, ${day} ${dayOfMon} ${month} ${year}"
    }

    //  handle the pull response received from the server
    //  pull response is a txt file that contains the files in global dir
    //  incl. the size and files count attributes
    fun receivePullResponse()
    {
        //  defines what is being recieved, FILESIZE OR FILEDATA
        var processes = listOf("size", "data")
        var currentProcess = processes[0]

        //  loop for receiving, tells the system to keep listening  fot incoming data
        var recv = true

        //  informs that file has been received, triggering next process
        var isDone = false

        //  expected file size
        //  initial is 8 because, we recv size then data, so size is hardcoded as 8 bytes -> String
        var expectedSize = 8

        //  tell server ready to start receiving
        socketWriter?.write("okay".toByteArray())
        while (recv)
        {
            var buffer = ByteArray(expectedSize)
            while (socketReader!!.available()>0)
            {
                isRequesting = true
                Log.d("Reading Socket", "Receiving Data: $currentProcess")
                socketReader?.readFully(buffer, 0, buffer.size)
                isDone = true
            }
            Log.d("Recv Loop", "Waiting for more data")

            //  processes the type of data received
            if (isDone)
            {
                if (currentProcess == "size")
                {
                    Log.d("Size of File!", buffer.decodeToString())
                }
                //  move to next process
                isDone = false

                //  send ack to server, ready to receive next
                socketWriter?.write("okay".toByteArray())

                //  check if data has been received, if yes then stop recv loop
                //  first we get the file size, then the data bytes, then close the loop
                if (currentProcess=="data")
                {
                    //  write data to file and save
                    var fo = FileOutputStream("/storage/emulated/0/SyncApp/global register.txt")
                    fo.write(buffer)
                    fo.close()
                    isRequesting = false
                    return
                }

                //  set received file size as the size of the data to follow
                //  meaning the bytes we receive in first loop are the one assigned
                //  as the amount we expect to receive as the file data
                expectedSize = buffer.decodeToString().toInt()

                //  meaning, now receive data
                currentProcess = processes[1]
            }


        }
    }

    //  compiles the contents of the local directory at app launch
    //  and assigns the attributes into the ui
    @RequiresApi(Build.VERSION_CODES.O)
    fun getLocalAttributes()
    {
        var localFiles = mutableListOf<String>()
        var path = "/storage/emulated/0/SyncApp/Music/"
        var size = 0

        //  walk the local directory to get all files
        Files.walk(Paths.get(path))
            .forEach(
                {Log.d("files: ", it.toString()); localFiles.add(it.toString()); size+=(Files.size(it)).toInt()}
            )

        Log.d("local size: ", size.toString())
        var fileCount = 0
        var dirCount = 0

        //  counts number of files and directories found
        for (localFile in localFiles)
        {
            var f = File(localFile)
            if(f.isFile)
            {
                fileCount += 1
            } else if(f.isDirectory)
            {
                dirCount += 1
            }


        }

        localFilesCount = fileCount
        localDirsCount = dirCount
        localSize = size
        Log.d("count", fileCount.toString())
        updateAttributes()
    }

    //  update the ui attributes
    fun updateAttributes()
    {
        _uiState.update { SyncUiState(localFilesCount = localFilesCount, localDirsCount = localDirsCount, localSize = localSize) }
    }

    //  gets all files in local directory, so they can be compared with ones in global directory
    @RequiresApi(Build.VERSION_CODES.O)
    fun getLocalFiles()
    {
        var localFiles = mutableListOf<String>()
        var path = "/storage/emulated/0/SyncApp/Music/"

        //  walks  the local directory
        Files.walk(Paths.get(path))
            .forEach(
                {Log.d("files: ", it.toString()); localFiles.add(it.toString())}
            )

        compareFiles(localFiles)
    }

    //  compares the contents of the local file and global and compiles a missing files list
    @RequiresApi(Build.VERSION_CODES.O)
    fun compareFiles(localFiles: List<String>)
    {
        //  compile files in global register
        var globalFilesWithPath = File("/storage/emulated/0/SyncApp/global register.txt")
        var globalFile = File("/storage/emulated/0/SyncApp/global register.txt")
        var tempGlobalSongs = BufferedReader(FileReader(globalFile)).readLines()
        var globalFSongs = mutableListOf<String>()

        //  get attributes from global register file and remove them
        globalFilesCount = tempGlobalSongs[(tempGlobalSongs.size)-3].split("##").last().toInt()
        globalDirsCount = tempGlobalSongs[(tempGlobalSongs.size)-2].split("##").last().toInt()
        globalSize = tempGlobalSongs[(tempGlobalSongs.size)-1].split("##").last().toInt()

        Log.d("files: ", globalFilesCount.toString())
        Log.d("dirs: ", globalDirsCount.toString())
        Log.d("size: ", globalSize.toString())

        //  remove attributes from global file
        var globalSongs = tempGlobalSongs.dropLast(3)

        globalSongs.forEach { globalFSongs.add(it)}

        var globalSize = globalFile.length()

        //  compile files in global register
        var localFSongs = mutableListOf<String>()
        localFiles.forEach { localFSongs.add(it.split("/".toString()).last()) }

        //  remove the name of the directory
        //  which i didn't have time figure out a better way to exclude properly
        localFSongs.removeAt(0)

        //  get missing files
        globalFSongs.forEach {
            if(!localFSongs.contains(it.split("\\".toString()).last())) missingFiles.add(it)
        }

        //  create missing files txt
        var fo = FileOutputStream("/storage/emulated/0/SyncApp/missing files.txt")
        missingFiles.forEach { fo.write(it.toByteArray()); fo.write("\n".toByteArray()) }

        fo.close()
    }

    //  gets the size of a file, be it audio file, txt, video etc...
    fun getFileSize(path: String): String
    {
        var f = File(path)

        return  f.length().toString()
    }

    //  add characters to a string so that its size adds up to 8 bytes
    fun addTrailing(targetLength: Int, text: String, symbol: String): String
    {
        var temp: String = ""
        while (temp.length+text.length<targetLength)
        {
            temp+=symbol
        }
        return temp+text
    }

    //  updates the ui sync date value
    fun updateSyncDate()
    {
        getDateTime()
        _uiState.update { SyncUiState(syncDate=syncDate) }
    }

    //  updates the ui value at start up
    private fun initialUiUpdate()
    {
        _uiState.update { SyncUiState(syncDate=syncDate, globalFilesCount = globalFilesCount, globalDirsCount = globalDirsCount, globalSize = globalSize) }
    }

    //  send pull request
    @RequiresApi(Build.VERSION_CODES.O)
    fun makeSyncRequest()
    {
        isWorking = true
        statusString = "Synchronising"
        updateStatusString()

        //  send missing files list
        if(!isRequesting)
        {
            socketWriter?.write("sync".toByteArray())
            sendMissingFile()
        }
        getLocalAttributes()
        getUpToDate()
        updateGlobalAttributes()
        updateSyncDate()

        //  saves data to shared preferences
        saveData()
        isWorking = false
        statusString = "Sync Complete!"
        updateStatusString()
    }

    //  actual function to send missing files
    fun sendMissingFile()
    {
        Log.d("Confirmation", "To send missing files")

        //  prepare files to send
        //  expected file size
        var temp = getFileSize("/storage/emulated/0/SyncApp/missing files.txt")
        var fileSize = addTrailing(8, temp, "0")

        var fo = FileInputStream("/storage/emulated/0/SyncApp/missing files.txt")

        //  tell server ready to start
        receiveConfirmation("stage0: --missing files")
        socketWriter?.write("okay".toByteArray())

        //  send file size
        socketWriter?.write(fileSize.toByteArray())

        //  wait for confirmation to send missing fileś data
        receiveConfirmation("stage1: --missing files")
        Log.d("Confirmation", "To send missing files")

        var buffer = ByteArray(fileSize.toInt())
        fo.read(buffer, 0, buffer.size)

        //  send file size
        socketWriter?.write(buffer)

        receiveFiles()

    }
    var bufferXize: Int = 0
    var confirmation = false

    //  global values for sync
    var gxSize: Int = 0

    //  receives missing files, or handles synchronisation
    fun receiveFiles()
    {
        //  receives the index of the missing file to expect
        fileGXtype = "index"
        Executors.newSingleThreadExecutor().execute {
            receiveData(8)
        }

        var count = 0

        while (count<missingFiles.size)
        {

            //  handle
            gxSize = 8
            fileGXtype = "index"
            confirmation = false
            var startTime = System.currentTimeMillis()
            startTime = System.currentTimeMillis()
            while(true)
            {
                if (System.currentTimeMillis() - startTime > 1000 && !isReceiving)
                {
                    socketWriter?.write("okay".toByteArray())
                    startTime = System.currentTimeMillis()
                }
                if(confirmation)
                {
                    break
                }
            }
            var indexV = fileGXsize.toInt()

            for (missingFile in missingFiles)
            {
                if (("${indexV.toString()}**") in missingFile)
                {
                    fileGXname = missingFile
                    break
                }
            }
            statusString = fileGXname.split("\\".toString()).last()
            updateStatusString()

            //  receives the size of the missing file to expect
            gxSize = 8
            fileGXtype = "count"
            confirmation = false

            //  to keep the system from going out of sync,
            //  I implemented a double ack
            startTime = System.currentTimeMillis()
            while(true)
            {
                if (System.currentTimeMillis() - startTime > 1000 && !isReceiving)
                {
                    socketWriter?.write("okay".toByteArray())
                    startTime = System.currentTimeMillis()
                }
                if(confirmation)
                {
                    break
                }
            }

            //  receives the data of the missing file
            gxSize = fileGXsize.toInt()
            fileGXtype = "file"
            for (missingFile in missingFiles)
            {
                if (("${indexV.toString()}**") in missingFile)
                {
                    fileGXname = missingFile
                        break
                }
            }

            confirmation = false
            Log.d("File", "Receiving file data.")

            //  to keep the system from going out of sync,
            //  I implemented a double ack
            startTime = System.currentTimeMillis()
            while(true)
            {
                if (System.currentTimeMillis() - startTime > 1000 && !isReceiving)
                {
                    socketWriter?.write("okay".toByteArray())
                    startTime = System.currentTimeMillis()
                }
                if(confirmation)
                {
                    break
                }
            }

            count+=1
        }


    }
    var fileGXtype = "index"
    var fileGXname = "xxy"
    var fileGXsize: String = "0"

    //  loop that listens for incoming data
    fun receiveData(bufferSize: Int): String
    {
        //  defines what is being recieved, FILESIZE OR FILEDATA
        var recv = true
        Log.d("Receive loop", "File-size: $gxSize")

        //  informs that file has been received, triggering next process
        var isDone = false

        while (true)
        {
            var buffer = ByteArray(gxSize)
            var type = fileGXtype
            Log.d("Size ", "${buffer.size}")

            //  blocks, until all expected data has been received
            while (socketReader!!.available() > 0) {
                isRequesting = true
                isReceiving = true
                bufferXize = buffer.size

                socketReader?.readFully(buffer, 0, buffer.size)

                isDone = true

            }

            //  if buffer size amount of data is received, process it accordingly

            //  saves the data as audio file
            if (isDone && (type =="file"))
            {
                var fileName = fileGXname
                var tempFilename = fileName.split("\\".toString()).last()
                var fo = FileOutputStream("/storage/emulated/0/SyncApp/Music/${tempFilename}")
                fo.write(buffer)
                fo.close()
                isReceiving = false
                isDone = false
                confirmation = true
                Log.d("File synced ", "File has been recieved")
                localFilesCount += 1
                localSize += buffer.size
                updateAttributes()

            }   else if (isDone && !(type =="file"))
            {
                //  if size of file is received
                isReceiving = false
                isDone = false
                fileGXsize = buffer.decodeToString()
                confirmation = true
                Log.d("Received ", "filesize: $fileGXsize")
            }

        }
        return ""
    }

    //  save data to shared preferences
    private fun saveData()
    {
        val sharedPreferences = context?.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor = sharedPreferences?.edit()

        editor?.putString("SyncDate", syncDate)
        editor?.putInt("globalFiles", globalFilesCount)
        editor?.putInt("globalDirs", globalDirsCount)
        editor?.putInt("globalSize", globalSize)

        editor?.apply()
    }

    //  load data from shared preferences
    private fun loadData()
    {
        val sharedPreferences = context?.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        syncDate = sharedPreferences?.getString("SyncDate", "---").toString()
        globalFilesCount = sharedPreferences?.getInt("globalFiles", 0) ?: 0
        globalDirsCount = sharedPreferences?.getInt("globalDirs", 0) ?: 0
        globalSize = sharedPreferences?.getInt("globalSize", 0) ?: 0
    }

    //  stops system from invoking requests if system is already pulling or syncing
    var isReceiving = false

    //  function that enforces the double sync tecnhique, if no ack is received then keep listening
    //  and requesting
    private fun receiveConfirmation(stage: String)
    {
        //  defines process type requiring confirmation, either index, size or data
        var currentProcess = "count"

        //  loop for receiving
        var recv = true
        Log.d("Confirmation wait", "Receiving Data: $stage")

        //  informs that file has been received, triggering next process
        var isDone = false

        //  await confirmation acknowledgement
        while (!isDone)
        {

            var buffer = ByteArray(4)
            while (socketReader!!.available() > 0) {
                isRequesting = true
                Log.d("Confirm Wait Socket", "Receiving Data: $currentProcess")
                socketReader?.readFully(buffer, 0, buffer.size)
                if (buffer.decodeToString() == "okay")
                {
                    isDone = true
                    confirmation = true
                    Log.d("Confirm Wait Socket", "Confirmation: ${buffer.decodeToString()}")
                }

            }

        }
    }
}