package com.pepkyc.cleevio.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.AsyncTask
import com.pepkyc.cleevio.activity.FileBrowserActivity
import kotlinx.android.synthetic.main.activity_file_browser.*
import java.io.File
import java.lang.ref.WeakReference

class BrowserViewHolder(val currPath : File, val selectedItems : MutableList<File>, isExitable : Boolean) : ViewModel() {

    val files: MutableLiveData
        get() {
            if (field == null) {

            }
        }


    class FetchDataTask() : AsyncTask<File, Void, List<MutableList<File>>>() {
        override fun doInBackground(vararg params: File?): List<MutableList<File>> {
            var arrayAll: Array<File>? = params[0]?.listFiles()
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

        }
    }
}