package com.pepkyc.cleevio.listeners

import android.view.Menu
import android.view.MenuItem
import android.util.Log
import android.widget.Toast
import com.pepkyc.cleevio.activity.FileBrowserActivity
import com.pepkyc.cleevio.adapters.FileAdapter
import java.io.File

/**
 * Listener sloužící k manipulaci s CAB
 */
class ActionModeCallbacksListener(val activity: FileBrowserActivity) : android.support.v7.view.ActionMode.Callback {

    override fun onActionItemClicked(mode: android.support.v7.view.ActionMode?, item: MenuItem?): Boolean {
        val notRemoved = mutableListOf<File>()
        for (file in activity.adapter!!.selectedItems) {
            if(!activity.deleteFile(file)){
                Toast.makeText(activity, "Error deleting: " + file.path, Toast.LENGTH_SHORT).show()
                notRemoved.add(file)
            }
        }
        activity.adapter!!.selectedItems = notRemoved
        mode?.finish()
        return true
    }

    override fun onCreateActionMode(mode: android.support.v7.view.ActionMode?, menu: Menu?): Boolean {
        activity.adapter!!.multiSelectCAB = true
        menu?.add("Delete")
        return true
    }

    override fun onPrepareActionMode(mode: android.support.v7.view.ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: android.support.v7.view.ActionMode?) {
        activity.adapter!!.multiSelectCAB = false
        if (activity.adapter!!.selectedItems.size != 0) {
            var toRemove = mutableListOf<Int>()
            for (file in activity.adapter!!.selectedItems) {
                toRemove.add(activity.viewIndexOf(file))
            }
            activity.adapter!!.selectedItems.clear()
            for (viewIndex in toRemove){
                activity.adapter!!.notifyItemChanged(viewIndex)
            }
        }
    }
}