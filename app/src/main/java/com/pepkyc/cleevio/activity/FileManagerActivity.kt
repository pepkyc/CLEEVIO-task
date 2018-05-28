package com.pepkyc.cleevio.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.adapters.FileAdapter
import com.pepkyc.cleevio.fragment.FileBrowserFragment
import com.pepkyc.cleevio.listeners.ActionModeCallbacksListener
import com.pepkyc.cleevio.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.activity_file_browser.*
import java.io.File
import android.support.v4.content.FileProvider



/**
 * Activity sloužící k prohledávání filesystému
 */
class FileManagerActivity : AppCompatActivity(), FileAdapter.BrowseInteractionListener {

    companion object {
        val DEFAULT_DIRECTORY_KEY = "defdir"
    }

    var browserViewModel: BrowserViewModel? = null
    private val actionModeCallbacks = ActionModeCallbacksListener(this)
    val fragment = FileBrowserFragment.newInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)
        setSupportActionBar(toolbar)
        browserViewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()


        if (browserViewModel!!.selectedItems.size != 0 && browserViewModel!!.multiSelectCAB) {
            startSupportActionMode(actionModeCallbacks)
        }
    }

    override fun onBackPressed() {
        if (browserViewModel!!.currPath.path?.compareTo("/") == 0) {
            super.onBackPressed()
        } else {
            fragment.shiftFolderBack()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.file_manager_options_menu, menu)
        return true
    }



    /**
     * Smaže složku a upraví adapter/recyclerview
     */
    fun deleteFile(file: File): Boolean {
        val success = file.delete()
        return if (success) {
            if (file.isDirectory) {
                val index = fragment.adapter!!.folders.indexOf(file)
                fragment.adapter!!.folders.removeAt(index)
                fragment.adapter!!.notifyItemRemoved(index)
            } else {
                val index = fragment.adapter!!.files.indexOf(file)
                fragment.adapter!!.files.removeAt(index)
                fragment.adapter!!.notifyItemRemoved(index + fragment.adapter!!.folders.size)
            }
            true
        } else {
            false
        }
    }

    override fun onFolderClicked(folder: File) {
    }

    override fun onFileClicked(file: File) {
        openInDefaultAcivity(file)
    }

    override fun onFolderLongClicked(folder: File, viewHolder: RecyclerView.ViewHolder) {
        if (!browserViewModel!!.multiSelectCAB) {
            startSupportActionMode(actionModeCallbacks)
        }
        fragment.adapter!!.selectItemCAB(folder, viewHolder)
    }

    override fun onFileLongClicked(file: File, viewHolder: RecyclerView.ViewHolder) {
        if (!browserViewModel!!.multiSelectCAB) {
            startSupportActionMode(actionModeCallbacks)
        }
        fragment.adapter!!.selectItemCAB(file, viewHolder)
    }

    /**
     * Otevře nebo zobrazí activity, které jsou schopny otevřít daný soubor
     */
    private fun openInDefaultAcivity(file: File) {
        val myMime = MimeTypeMap.getSingleton()
        val newIntent = Intent(Intent.ACTION_VIEW)
        val extension = fileExt(file.name)
        val mimeType = myMime.getMimeTypeFromExtension(extension)

        val uri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(this, applicationContext.packageName + ".com.pepkyc.cleevio", file)
        }else{
            Uri.fromFile(file)
        }

        newIntent.setDataAndType(uri, mimeType)
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(Intent.createChooser(newIntent, "Open with:"))
    }

    private fun fileExt(urlv: String): String? {
        var url = urlv
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"))
        }
        return if (url.lastIndexOf(".") == -1) {
            null
        } else {
            var ext = url.substring(url.lastIndexOf(".") + 1)
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"))
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"))
            }
            ext.toLowerCase()

        }
    }
}
