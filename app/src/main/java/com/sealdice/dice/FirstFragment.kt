package com.sealdice.dice

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.sealdice.dice.common.ExtractAssets
import com.sealdice.dice.common.FileWrite
import com.sealdice.dice.databinding.FragmentFirstBinding
import com.sealdice.dice.utils.Utils
import com.sealdice.dice.utils.ViewModelMain
import kotlinx.coroutines.*
import java.io.File
import kotlin.system.exitProcess


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private lateinit var _binding: FragmentFirstBinding

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
    private var shellLogs = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var isrun = false
        val packageManager = this.activity?.packageManager
        val packageName = this.activity?.packageName
        val packageInfo = packageName?.let { packageManager?.getPackageInfo(it, 0) }
        val versionName = packageInfo?.versionName
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val nightModeFlags = context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        binding.textviewFirst.text = StringBuilder(binding.textviewFirst.text).append("\nApp Version: $versionName")
        binding.buttonThird.setOnClickListener {
            val address = sharedPreferences?.getString("ui_address", "http://127.0.0.1:3211")
            if (sharedPreferences?.getBoolean("use_internal_webview", true) == true) {
                val intent = Intent(context, WebViewActivity::class.java)
                intent.putExtra("url", address)
                startActivity(intent)
            } else {
                val uri = Uri.parse(address)
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                intent.data = uri
                startActivity(intent)
            }
        }
        binding.buttonExit.setOnClickListener {
            this.activity?.let { it1 -> finishAffinity(it1) } // Finishes all activities.
            exitProcess(0)
        }
        binding.buttonConsole.setOnClickListener {
            val alertDialogBuilder = context?.let { it1 ->
                AlertDialog.Builder(
                    it1, R.style.Theme_Mshell_DialogOverlay
                )
            }
            alertDialogBuilder?.setTitle("控制台")
            alertDialogBuilder?.setMessage(shellLogs)
            alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->
            }
            alertDialogBuilder?.create()?.show()
        }
        binding.buttonReset.setOnClickListener {
            val alertDialogBuilder = context?.let { it1 ->
                AlertDialog.Builder(
                    it1, R.style.Theme_Mshell_DialogOverlay
                )
            }
            alertDialogBuilder?.setTitle("警告")
            alertDialogBuilder?.setMessage("此操作将抹除本地存储的所有数据并且无法恢复\n如果你不明白此按钮的作用请点取消\n返回请按”取消“ 继续请按”确定“")
            alertDialogBuilder?.setNegativeButton("取消") {_: DialogInterface, _: Int ->}
            alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->
                context?.let { it1 -> FileWrite.getPrivateFileDir(it1)+"sealdice/" }?.let { it2 -> delete(it2) }
                context?.let { it1 ->
                    this.view?.let { it2 ->
                        Snackbar.make(
                            it1, it2,"清除成功", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            alertDialogBuilder?.create()?.show()
        }
        binding.buttonOutput.setOnClickListener {
            FileWrite.FileCount = 0
            val permissionState =
                context?.let { it1 -> ContextCompat.checkSelfPermission(it1, Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(context, "已授权！", Toast.LENGTH_LONG).show()
                val builder: AlertDialog.Builder? = context?.let { it1 -> AlertDialog.Builder(it1, R.style.Theme_Mshell_DialogOverlay) }
                builder?.setCancelable(false) // if you want user to wait for some process to finish,
                builder?.setView(R.layout.layout_loading_dialog)
                val dialog = builder?.create()
                dialog?.show()
                GlobalScope.launch(context = Dispatchers.IO) {
                    context?.let { it1 -> FileWrite.getPrivateFileDir(it1)+"sealdice/" }
                        ?.let { it2 -> File(it2) }
                        ?.let { it3 -> FileWrite.copyFolder(it3,File("${FileWrite.SDCardDir}/Documents/${context?.packageName}/sealdice/")) }
                    dialog?.dismiss()
                    val alertDialogBuilder = context?.let { it1 ->
                        AlertDialog.Builder(
                            it1, R.style.Theme_Mshell_DialogOverlay
                        )
                    }
                    alertDialogBuilder?.setTitle("提示")
                    alertDialogBuilder?.setMessage("所有内部数据已经导出至\n"+"${FileWrite.SDCardDir}/Documents/${context?.packageName}/sealdice/\n共${FileWrite.FileCount}个文件")
                    alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int -> }
                    withContext(Dispatchers.Main) {
                        alertDialogBuilder?.create()?.show()
                    }
                }
            } else {
//                Toast.makeText(context, "未获得文件权限！", Toast.LENGTH_LONG).show()
                this.view?.let { it2 ->
                    context?.let { it1 ->
                        Snackbar.make(
                            it1, it2,"未获得文件权限！", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                this.activity?.let { it1 -> ActivityCompat.requestPermissions(it1, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 1) }
                val alertDialogBuilder = context?.let { it1 ->
                    AlertDialog.Builder(
                        it1
                    )
                }
                alertDialogBuilder?.setTitle("提示")
                alertDialogBuilder?.setMessage("请授权文件读写权限以使用此功能\n授权后请重试")
                alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->}
                alertDialogBuilder?.create()?.show()
            }
        }
        binding.buttonInput.setOnClickListener {
            val alertDialogBuilder = context?.let { it1 ->
                AlertDialog.Builder(
                    it1, R.style.Theme_Mshell_DialogOverlay
                )
            }
            alertDialogBuilder?.setTitle("警告")
            alertDialogBuilder?.setMessage("将从\n"+"${FileWrite.SDCardDir}/Documents/${context?.packageName}/sealdice/\n中导入数据，内部存储中所有的重复文件将被覆盖，覆盖后将无法恢复\n返回请按”取消“ 继续请按”确定“")
            alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->
                FileWrite.FileCount = 0
                val permissionState =
                    context?.let { it1 -> ContextCompat.checkSelfPermission(it1, Manifest.permission.READ_EXTERNAL_STORAGE) }
                if (permissionState == PackageManager.PERMISSION_GRANTED) {
                    val builder: AlertDialog.Builder? = context?.let { it1 -> AlertDialog.Builder(it1) }
                    builder?.setCancelable(false) // if you want user to wait for some process to finish,
                    builder?.setView(R.layout.layout_loading_dialog)
                    val dialog = builder?.create()
                    dialog?.show()
                    GlobalScope.launch(context = Dispatchers.IO) {
                        context?.let { it1 -> FileWrite.getPrivateFileDir(it1)+"sealdice" }?.let { it2 ->
                            File(
                                it2
                            )
                        }?.let { it3 ->
                            FileWrite.copyFolder(File("${FileWrite.SDCardDir}/Documents/${context?.packageName}/sealdice/"),
                                it3
                            )
                        }
                        dialog?.dismiss()
                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                context, "导入了${FileWrite.FileCount}个文件", Toast.LENGTH_LONG
//                            ).show()
                            context?.let { it1 ->
                                Snackbar.make(
                                    it1, view,"导入了${FileWrite.FileCount}个文件", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
//                    Toast.makeText(context, "未获得文件权限！", Toast.LENGTH_LONG).show()
                    context?.let { it1 ->
                        Snackbar.make(
                            it1, view,"未获得文件权限！", Toast.LENGTH_SHORT
                        ).show()
                    }
                    this.activity?.let { it1 -> ActivityCompat.requestPermissions(it1, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 1) }
                    val alertDialogBuilder2 = context?.let { it1 ->
                        AlertDialog.Builder(
                            it1, R.style.Theme_Mshell_DialogOverlay
                        )
                    }
                    alertDialogBuilder2?.setTitle("提示")
                    alertDialogBuilder2?.setMessage("请授权文件读写权限以使用此功能\n授权后请重试")
                    alertDialogBuilder2?.setPositiveButton("确定") { _: DialogInterface, _: Int ->}
                    alertDialogBuilder2?.create()?.show()
                }
            }
            alertDialogBuilder?.setNegativeButton("取消") {_: DialogInterface, _: Int ->}
            alertDialogBuilder?.create()?.show()
        }
        binding.buttonFirst.setOnClickListener {
            if (isrun) {
                shellLogs += "sealdice is running"
                val alertDialogBuilder = context?.let { it1 ->
                    AlertDialog.Builder(
                        it1, R.style.Theme_Mshell_DialogOverlay
                    )
                }
                alertDialogBuilder?.setTitle("提示")
                alertDialogBuilder?.setMessage("请先重启APP后再启动海豹核心")
                alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->
                }
                alertDialogBuilder?.create()?.show()
            } else {
                isrun = true
                if (sharedPreferences?.getBoolean("extract_on_start", true) == true) {
                    ExtractAssets(context).extractResources("sealdice")
                }
                val args = sharedPreferences?.getString("launch_args", "")
                execShell("cd sealdice&&./sealdice-core $args",true)
                binding.buttonInput.visibility = View.GONE
                binding.buttonOutput.visibility = View.GONE
                binding.buttonReset.visibility = View.GONE
                binding.buttonSecond.visibility = View.VISIBLE
                binding.buttonThird.visibility = View.VISIBLE
                binding.buttonConsole.visibility = View.VISIBLE
                binding.buttonFirst.visibility = View.GONE
                if (!launchAliveService(context)) {
                    val alertDialogBuilder = context?.let { it1 ->
                        AlertDialog.Builder(
                            it1, R.style.Theme_Mshell_DialogOverlay
                        )
                    }
                    alertDialogBuilder?.setTitle("提示")
                    alertDialogBuilder?.setMessage("似乎并没有开启任何保活策略，这可能导致后台被清理")
                    alertDialogBuilder?.setPositiveButton("确定") { _: DialogInterface, _: Int ->}
                    alertDialogBuilder?.create()?.show()
                }
                GlobalScope.launch(context = Dispatchers.IO) {
                    for (i in 0..10) {
                        withContext(Dispatchers.Main) {
                            binding.textviewFirst.text = "正在启动...\n请等待${10 - i}s..."
                        }
                        Thread.sleep(1000)
                    }
                    withContext(Dispatchers.Main){
                        binding.textviewFirst.text = "启动完成（或者失败）"
                    }
                    val address = sharedPreferences?.getString("ui_address", "http://127.0.0.1:3211")
                    if (sharedPreferences?.getBoolean("use_internal_webview", true) == true) {
                        val intent = Intent(context, WebViewActivity::class.java)
                        intent.putExtra("url", address)
                        startActivity(intent)
                    } else {
                        val uri = Uri.parse(address)
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        intent.data = uri
                        startActivity(intent)
                    }
                }
            }
            binding.buttonSecond.setOnClickListener {
                binding.buttonSecond.visibility = View.GONE
                this.activity?.stopService(Intent(context, NotificationService::class.java))
                this.activity?.stopService(Intent(context, MediaService::class.java))
                this.activity?.stopService(Intent(context, WakeLockService::class.java))
                this.activity?.stopService(Intent(context, FloatWindowService::class.java))
                execShell("pkill -SIGINT sealdice-core",false)
                binding.buttonExit.visibility = View.VISIBLE
            }
        }
    }

    private fun launchAliveService(context: Context?) : Boolean{
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        var executed = false
        if (sharedPreferences != null) {
            if (sharedPreferences.getBoolean("alive_notification", true)) {
                val intentNoti = Intent(context, NotificationService::class.java)
                context.startService(intentNoti)
                executed = true
            }
            if (sharedPreferences.getBoolean("alive_media", false)) {
                val intentMedia = Intent(context, MediaService::class.java)
                context.startService(intentMedia)
                executed = true
            }
            if (sharedPreferences.getBoolean("alive_wakelock", true)) {
                val intentWakelock = Intent(context, WakeLockService::class.java)
                context.startService(intentWakelock)
                executed = true
            }
            if (sharedPreferences.getBoolean("alive_floatwindow", false)) {
                context.startService(Intent(context, FloatWindowService::class.java))
                this.activity?.let {
                    Utils.checkSuspendedWindowPermission(it) {
                        ViewModelMain.isShowSuspendWindow.postValue(true)
                        ViewModelMain.isVisible.postValue(true)
                    }
                }
                executed = true
            }
        }
        return executed
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun execShell(cmd: String, recordLog: Boolean) {
        GlobalScope.launch(context = Dispatchers.IO) {
            val process = Runtime.getRuntime().exec("sh")
            val os = process.outputStream
            os.write("cd ${context?.filesDir?.absolutePath}&&".toByteArray())
            os.write(cmd.toByteArray())
            os.flush()
            os.close()
//            Thread.sleep(3000)
//            val data = process.inputStream.readBytes()
//            val error = process.errorStream.readBytes()
//            if (data.isNotEmpty()) {
//                shellLogs += String(data)
//                shellLogs += "\n"
//            } else {
//                shellLogs += String(error)
//                shellLogs += "\n"
//            }
//            Log.i("ExecShell", shellLogs)
//            withContext(Dispatchers.Main) {
//                binding.textviewFirst.text = shellLogs
//            }
            while (recordLog) {
                val data = process.inputStream.readBytes()
                val error = process.errorStream.readBytes()
                if (data.isNotEmpty()) {
                    shellLogs += String(data)
                    shellLogs += "\n"
                } else {
                    shellLogs += String(error)
                    shellLogs += "\n"
                }
                Log.i("ExecShell", shellLogs)
                withContext(Dispatchers.Main) {
                binding.textviewFirst.text = shellLogs
                }
                Thread.sleep(1000)
            }
        }
    }

    /** 删除文件，可以是文件或文件夹
     * @param delFile 要删除的文件夹或文件名
     * @return 删除成功返回true，否则返回false
     */
    private fun delete(delFile: String): Boolean {
        val file = File(delFile)
        return if (!file.exists()) {
//            Toast.makeText(
//                ApplicationProvider.getApplicationContext<Context>(),
//                "删除文件失败:" + delFile + "不存在！",
//                Toast.LENGTH_SHORT
//            ).show()
            false
        } else {
            if (file.isFile) deleteSingleFile(delFile) else deleteDirectory(delFile)
        }
    }

    /** 删除单个文件
     * @param filePath 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    private fun deleteSingleFile(filePath: String): Boolean {
        val file = File(filePath)
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        return if (file.exists() && file.isFile) {
            if (file.delete()) {
                Log.e(
                    "--Method--",
                    "Copy_Delete.deleteSingleFile: 删除单个文件" + filePath + "成功！"
                )
                true
            } else {
//                Toast.makeText(
//                    ApplicationProvider.getApplicationContext<Context>(),
//                    "删除单个文件" + `filePath$Name` + "失败！",
//                    Toast.LENGTH_SHORT
//                ).show()
                false
            }
        } else {
//            Toast.makeText(
//                ApplicationProvider.getApplicationContext<Context>(),
//                "删除单个文件失败：" + `filePath$Name` + "不存在！",
//                Toast.LENGTH_SHORT
//            ).show()
            false
        }
    }

    /** 删除目录及目录下的文件
     * @param filePath 要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    private fun deleteDirectory(_filePath: String): Boolean {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        var filePath = _filePath
        if (!filePath.endsWith(File.separator)) filePath += File.separator
        val dirFile = File(filePath)
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory) {
//            Toast.makeText(
//                ApplicationProvider.getApplicationContext<Context>(),
//                "删除目录失败：" + filePath + "不存在！",
//                Toast.LENGTH_SHORT
//            ).show()
            return false
        }
        var flag = true
        // 删除文件夹中的所有文件包括子目录
        val files: Array<File> = dirFile.listFiles() as Array<File>
        for (file in files) {
            // 删除子文件
            if (file.isFile) {
                flag = deleteSingleFile(file.absolutePath)
                if (!flag) break
            } else if (file.isDirectory) {
                flag = deleteDirectory(
                    file.absolutePath
                )
                if (!flag) break
            }
        }
        if (!flag) {
//            Toast.makeText(
//                ApplicationProvider.getApplicationContext<Context>(),
//                "删除目录失败！",
//                Toast.LENGTH_SHORT
//            ).show()
            return false
        }
        // 删除当前目录
        return if (dirFile.delete()) {
            Log.e("--Method--", "Copy_Delete.deleteDirectory: 删除目录" + filePath + "成功！")
            true
        } else {
//            Toast.makeText(
//                ApplicationProvider.getApplicationContext<Context>(),
//                "删除目录：" + filePath + "失败！",
//                Toast.LENGTH_SHORT
//            ).show()
            false
        }
    }
}