package com.pepkyc.cleevio.listeners

import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.pepkyc.cleevio.activity.FileManagerActivity
import java.io.File

/**
 * Listener sloužící k manipulaci s CAB
 */
class ActionModeCallbacksListener(val activity: FileManagerActivity) : android.support.v7.view.ActionMode.Callback {
    override fun onActionItemClicked(mode: android.support.v7.view.ActionMode?, item: MenuItem?): Boolean {
        val notRemoved = mutableListOf<File>()
        for (file in activity.browserViewModel!!.selectedItems) {
            if(!activity.deleteFile(file)){
                Toast.makeText(activity, "Error deleting: " + file.path, Toast.LENGTH_SHORT).show()
                notRemoved.add(file)
            }
        }
        activity.browserViewModel!!.selectedItems = notRemoved
        mode?.finish()
        return true
    }

    override fun onCreateActionMode(mode: android.support.v7.view.ActionMode?, menu: Menu?): Boolean {
        activity.browserViewModel!!.multiSelectCAB = true
        menu?.add("Delete")
        return true
    }

    override fun onPrepareActionMode(mode: android.support.v7.view.ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: android.support.v7.view.ActionMode?) {
        activity.browserViewModel!!.multiSelectCAB = false
        val selectedItems = activity.browserViewModel!!.selectedItems
        if (selectedItems.size != 0) {
            val toRemove = mutableListOf<Int>()
            for (file in selectedItems) {
                toRemove.add(viewIndexOf(file))
            }
            selectedItems.clear()
            activity.browserViewModel!!.selectedItems
            for (viewIndex in toRemove){
                activity.fragment.adapter!!.notifyItemChanged(viewIndex)
            }
        }
    }

    /**
     * Index souboru v recyclerView
     */
    fun viewIndexOf(file: File): Int {
        return if (file.isDirectory) {
            activity.fragment.adapter!!.folders.indexOf(file)
        } else {
            activity.fragment.adapter!!.files.indexOf(file) + activity.fragment.adapter!!.folders.size
        }

    }
}