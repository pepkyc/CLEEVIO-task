package com.pepkyc.cleevio.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_file_browser.*
import java.io.File
import com.pepkyc.cleevio.R
import android.webkit.MimeTypeMap
import com.pepkyc.cleevio.adapters.FileAdapter
import com.pepkyc.cleevio.listeners.ActionModeCallbacksListener
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.support.v7.widget.RecyclerView
import android.widget.EditText

/**
 * Activity sloužící k prohledávání filesystému
 */
class FileBrowserActivity : AppCompatActivity(), FileAdapter.BrowseInteractionListener {
    private val PERMISSIONS_REQUEST_READ_STORAGE = 24

    companion object {
        val DEFAULT_DIRECTORY_KEY = "defdir"
    }

    private var sharedPref: SharedPreferences? = null
    private var currPath: File? = null
    var adapter: FileAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)
        setSupportActionBar(toolbar)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        currPath = File(sharedPref!!.getString(DEFAULT_DIRECTORY_KEY, Environment.getExternalStorageDirectory().path))
        if (currPath!!.parent == null) supportActionBar?.setDisplayHomeAsUpEnabled(false)
        val viewManager = if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            LinearLayoutManager(this)
        } else {
            GridLayoutManager(this, 6)
        }
        val actionModeCallbacks = ActionModeCallbacksListener(this)
        adapter = FileAdapter(this, actionModeCallbacks)
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
//            registerForContextMenu(this)
        }
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val controller =
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)

        recyclerView.layoutAnimation = controller
        recyclerView.adapter.notifyDataSetChanged()
        recyclerView.scheduleLayoutAnimation()
    }

    override fun onResume() {
        super.onResume()
        setDataAsync(currPath!!)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString("currPath", currPath?.path)
        outState?.putStringArrayList("selectedItems", toStringArrayList(adapter!!.selectedItems))
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        val curr = savedInstanceState?.getString("currPath")
        if (curr != null) {
            currPath = File(curr)
            if (currPath?.path?.compareTo("/") != 0) supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        val selectedItems = savedInstanceState?.getStringArrayList("selectedItems")
        if (selectedItems != null) {
            adapter!!.selectedItems = fromStringArrayList(selectedItems)
            adapter!!.multiSelectCAB = true
            startSupportActionMode(adapter!!.actionModeCallbacks)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                shiftFolderBack()
                return true
            }
            R.id.refresh -> {
                setDataAsync(currPath!!)
                return true
            }
            R.id.settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }
            R.id.set_path -> {
                setPathDialog()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Existují složky, kde složka předchozí není čitelná, tím pádem se k ní skrz proklikávání nelze dostat
     * Zde se dá nastavit přímo cesta k dané složce, tím pádem tato zábrana mizí
     */
    private fun setPathDialog() {
        val editText = EditText(this)
        editText.apply {
            setText(currPath!!.path.toString())
            setSelection(text.length)
        }

        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Set path manually")
            setPositiveButton("OK") { _, _ ->
                val file = File(editText.text.toString())
                when {
                    !file.exists() -> {
                        Toast.makeText(this@FileBrowserActivity, "Directory does not exist", Toast.LENGTH_SHORT).show()
                    }
                    !file.isDirectory -> {
                        Toast.makeText(this@FileBrowserActivity, "Directory is not a folder", Toast.LENGTH_SHORT).show()
                    }
                    !file.canRead() -> {
                        Toast.makeText(this@FileBrowserActivity, "Folder is not readable", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        currPath = file
                        setDataAsync(currPath!!)
                        if (file.parent != null) {
                            supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        }
                    }
                }
            }
            setView(editText)
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    override fun onBackPressed() {
        if (currPath?.path?.compareTo("/") == 0) {
            super.onBackPressed()
        } else {
            shiftFolderBack()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setDataAsync(currPath!!)
                } else {
                    Toast.makeText(this, getString(R.string.no_permission), Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun setDataAsync(inDirectory: File) {
        if (checkReadPermission()) {
            object : AsyncTask<String, Void, List<MutableList<File>>>() {
                override fun doInBackground(vararg params: String?): List<MutableList<File>> {
                    var arrayAll: Array<File>? = inDirectory.listFiles()
                    var folders = mutableListOf<File>()
                    var files = mutableListOf<File>()
                    if (arrayAll != null) {
                        val sorted = arrayAll.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                        sorted.forEach {
                            if (it.isDirectory) {
                                folders.add(it)
                            } else {
                                files.add(it)
                            }
                        }
                    }
                    return listOf(folders, files)
                }

                override fun onPostExecute(list: List<MutableList<File>>) {
                    adapter?.folders = list[0]
                    adapter?.files = list[1]
                    recycler_view.adapter = adapter
                    runLayoutAnimation(recycler_view)
                }
            }.execute()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) askReadPermission()
        }
    }

    /**
     * Smaže složku a upraví adapter/recyclerview
     */
    fun deleteFile(file: File): Boolean {
        val success = file.delete()
        return if (success) {
            if (file.isDirectory) {
                val index = adapter!!.folders.indexOf(file)
                adapter!!.folders.removeAt(index)
                adapter!!.notifyItemRemoved(index)
            } else {
                val index = adapter!!.files.indexOf(file)
                adapter!!.files.removeAt(index)
                adapter!!.notifyItemRemoved(index + adapter!!.folders.size)
            }
            true
        } else {
            false
        }
    }

    /**
     * Index souboru v recyclerView
     */
    fun viewIndexOf(file: File): Int {
        return if (file.isDirectory) {
            adapter!!.folders.indexOf(file)
        } else {
            adapter!!.files.indexOf(file) + adapter!!.folders.size
        }

    }

    override fun onFolderClicked(folder: File) {
        shiftFolderForth(folder.name)
    }

    override fun onFileClicked(file: File) {
        openInDefaultAcivity(file)
    }

    /**
     * Otevře nebo zobrazí activity, které jsou schopny otevřít daný soubor
     */
    private fun openInDefaultAcivity(file: File) {
        val intent = Intent()
        intent.action = android.content.Intent.ACTION_VIEW
        val mime = MimeTypeMap.getSingleton()
        val ext = file.name.substring(file.name.indexOf(".") + 1)
        val type = mime.getMimeTypeFromExtension(ext)
        intent.setDataAndType(Uri.fromFile(file), type)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "App not found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shiftFolderForth(name: String) {
        val file = File(currPath, name)
        if (file.canRead()) {
            currPath = file
            setDataAsync(currPath!!)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            Toast.makeText(this, "Can not read directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shiftFolderBack() {
        val nextPath = currPath?.parentFile ?: currPath
        if (nextPath!!.canRead()) {
            currPath = nextPath
            setDataAsync(currPath!!)
            if (currPath?.path?.compareTo("/") == 0) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        } else {
            Toast.makeText(this, "Can not read parent directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkReadPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun askReadPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_STORAGE)
    }

    /**
     * Slouží k obnoveni označených položek po změně orientace obrazovky
     */
    private fun fromStringArrayList(stringFiles : MutableList<String>): MutableList<File> {
        val fileList = mutableListOf<File>()
        stringFiles.forEach {
            fileList.add(File(it))
        }
        return fileList
    }

    /**
     * Slouží k obnoveni označených položek po změně orientace obrazovky
     */
    private fun toStringArrayList(files : List<File>): ArrayList<String> {
        val stringList = arrayListOf<String>()
        files.forEach {
            stringList.add(it.path.toString())
        }
        return stringList
    }
}
