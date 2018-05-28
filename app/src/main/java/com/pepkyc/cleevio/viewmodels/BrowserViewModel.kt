package com.pepkyc.cleevio.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import android.os.Environment
import android.preference.PreferenceManager
import com.pepkyc.cleevio.activity.FileManagerActivity.Companion.DEFAULT_DIRECTORY_KEY
import java.io.File

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    /*Indikuje jestli má být aplikace zavřena na dalším stisku tlačítka domů*/
    var isExitable: Boolean = false
    /*Indikuje, jestli máme vybrané soubory v CAB*/
    var multiSelectCAB = false

    var currPath: File = File(PreferenceManager.getDefaultSharedPreferences(application)!!.getString(DEFAULT_DIRECTORY_KEY, Environment.getExternalStorageDirectory().path))
        set(value) {
            field = value
            loadData()
        }

    val data: MutableLiveData<List<MutableList<File>>> = MutableLiveData()
    var selectedItems: MutableList<File> = mutableListOf()

    init {
        loadData()
    }

    /*Static field leak zde nemůže nastat, není zde reference do Context konkrétní activity (Pouze do Application context, což nevadí)*/
    @SuppressLint("StaticFieldLeak")
    private fun loadData() {
        object : AsyncTask<Void, Void, List<MutableList<File>>>() {
            override fun doInBackground(vararg params: Void?): List<MutableList<File>> {
                var arrayAll: Array<File>? = currPath.listFiles()
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
                data.value = list
            }
        }.execute()
    }
}