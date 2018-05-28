package com.pepkyc.cleevio.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.adapters.FileAdapter
import com.pepkyc.cleevio.fragment.FileBrowserFragment
import com.pepkyc.cleevio.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.activity_file_browser.*
import java.io.File
import android.app.Activity
import android.view.Menu


class SelectFolderActivity : AppCompatActivity(), FileAdapter.BrowseInteractionListener {

    companion object {
        val SELECTED_PATH = "path"
    }

    val fragment = FileBrowserFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_folder)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Choose path"
        supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_folder -> {
                val returnIntent = Intent()
                returnIntent.putExtra(SELECTED_PATH, fragment.browserViewModel!!.currPath.path)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.select_folder_options_menu, menu)
        return true
    }


    override fun onBackPressed() {
        if (fragment.browserViewModel!!.currPath.path?.compareTo("/") == 0) {
            super.onBackPressed()
        } else {
            fragment.shiftFolderBack()
        }
    }

    override fun onFolderClicked(folder: File) {
    }

    override fun onFileClicked(file: File) {
    }

    override fun onFolderLongClicked(folder: File, viewHolder: RecyclerView.ViewHolder) {

    }

    override fun onFileLongClicked(file: File, viewHolder: RecyclerView.ViewHolder) {
    }
}
