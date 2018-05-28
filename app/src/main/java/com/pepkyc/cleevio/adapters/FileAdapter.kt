package com.pepkyc.cleevio.adapters

import android.graphics.Color
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.viewmodels.BrowserViewModel
import java.io.File


/**
 * Adapter sloužící k naplněni recyclerView daty
 */
class FileAdapter(val listener: BrowseInteractionListener, var browserViewModel: BrowserViewModel, val actionModeCallbacks: ActionMode.Callback?, var folders: MutableList<File> = mutableListOf(), var files: MutableList<File> = mutableListOf()) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    companion object {
        val FOLDER_ITEM = 333
        val FILE_ITEM = 999
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < folders.size)
            FOLDER_ITEM
        else
            FILE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            FILE_ITEM -> {
                val fileView = LayoutInflater.from(parent.context).inflate(R.layout.file_view, parent, false)
                ViewHolderFile(fileView)
            }
            FOLDER_ITEM -> {
                val folderView = LayoutInflater.from(parent.context).inflate(R.layout.folder_view, parent, false)
                ViewHolderFolder(folderView)
            }
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return when (holder) {
            is ViewHolderFile -> {
                holder.bind(files[getFileDataPosition(position)])
            }
            is ViewHolderFolder -> {
                holder.bind(folders[position])
            }
            else -> throw RuntimeException()
        }
    }

    override fun getItemCount() = folders.size + files.size


    private fun getFileDataPosition(position: Int): Int {
        return position - folders.size
    }

    inner class ViewHolderFile(val view: View) : RecyclerView.ViewHolder(view) {
        private val fileName = view.findViewById<TextView>(R.id.file_name)!!

        fun bind(file: File) {
            fileName.text = file.name
            itemView.setOnClickListener {
                if (browserViewModel.multiSelectCAB) {
                    selectItemCAB(file, this)
                } else {
                    listener.onFileClicked(files[getFileDataPosition(adapterPosition)])
                }
            }

            itemView.setOnLongClickListener {
                listener.onFileLongClicked(file, this)
                true
            }



            actionModeCallbacks?.let {
            }
            if (browserViewModel.selectedItems.contains(file)) {
                itemView.setBackgroundColor(Color.LTGRAY)
            } else {
                itemView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    inner class ViewHolderFolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val folderName = view.findViewById<TextView>(R.id.folder_name)!!

        fun bind(file: File) {
            folderName.text = file.name
            itemView.setOnClickListener {
                if (browserViewModel.multiSelectCAB) {
                    selectItemCAB(file, this)
                } else {
                    listener.onFolderClicked(folders[adapterPosition])
                }
            }

            itemView.setOnLongClickListener {
                listener.onFolderLongClicked(file, this)
                true
            }


            actionModeCallbacks?.let {
            }
            if (browserViewModel.selectedItems.contains(file)) {
                itemView.setBackgroundColor(Color.LTGRAY)
            } else {
                itemView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    /*Označí/odoznačí položku v recyclerview*/
    fun selectItemCAB(file: File, viewHolder: RecyclerView.ViewHolder) {
        val selecterItems = browserViewModel.selectedItems
        if (selecterItems.contains(file)) {
            selecterItems.remove(file)
            viewHolder.itemView.setBackgroundColor(Color.WHITE)
        } else {
            selecterItems.add(file)
            viewHolder.itemView.setBackgroundColor(Color.LTGRAY)
        }
    }

    /**
     * Listener sloužící k přenesení logiky manipulace s daty pryč z Adapter
     */
    interface BrowseInteractionListener {
        fun onFolderClicked(folder: File)
        fun onFileClicked(file: File)
        fun onFolderLongClicked(folder: File, viewHolder: RecyclerView.ViewHolder)
        fun onFileLongClicked(file: File, viewHolder: RecyclerView.ViewHolder)
    }

}
