package com.pepkyc.cleevio.fragment

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.adapters.FileAdapter
import com.pepkyc.cleevio.listeners.ActionModeCallbacksListener
import com.pepkyc.cleevio.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.fragment_file_browser_fragment.*
import java.io.File

class FileBrowserFragment : Fragment(), FileAdapter.BrowseInteractionListener {
    private val PERMISSIONS_REQUEST_READ_STORAGE = 24

    var browserViewModel: BrowserViewModel? = null
    private var sharedPref: SharedPreferences? = null
    var adapter: FileAdapter? = null
    private var actionModeCallback: ActionModeCallbacksListener? = null
    private var supportActivity: AppCompatActivity? = null
    private var listener : FileAdapter.BrowseInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_browser_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        supportActivity = activity as AppCompatActivity

        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        browserViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(BrowserViewModel::class.java)

        if (browserViewModel!!.currPath.parent != null) supportActivity!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        browserViewModel!!.data.observe(this, Observer<List<MutableList<File>>> {
            adapter?.folders = it!![0]
            adapter?.files = it[1]
            recycler_view.adapter = adapter
            runLayoutAnimation(recycler_view)
            pathView.text = browserViewModel!!.currPath.path
        })

        val viewManager = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LinearLayoutManager(activity)
        } else {
            GridLayoutManager(activity, 6)
        }
        adapter = FileAdapter(this, browserViewModel!!, actionModeCallback)
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            setEmptyView(list_empty)
        }

        actionModeCallback?.let {
            if (browserViewModel!!.selectedItems.size != 0 && browserViewModel!!.multiSelectCAB) {
                supportActivity!!.startSupportActionMode(it)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as FileAdapter.BrowseInteractionListener
    }

    override fun onStart() {
        super.onStart()
        setData(browserViewModel!!.currPath)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                shiftFolderBack()
                return true
            }
            R.id.refresh -> {
                setData(browserViewModel!!.currPath)
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


    companion object {
        @JvmStatic
        fun newInstance() = FileBrowserFragment()
    }

    /**
     * Existují složky, kde složka předchozí není čitelná, tím pádem se k ní skrz proklikávání nelze dostat
     * Zde se dá nastavit přímo cesta k dané složce, tím pádem tato zábrana mizí
     */
    private fun setPathDialog() {
        val editText = EditText(activity)
        editText.apply {
            setText(browserViewModel!!.currPath.path.toString())
            setSelection(text.length)
        }

        val builder = AlertDialog.Builder(activity!!)
        builder.apply {
            setTitle("Set path manually")
            setPositiveButton("OK") { _, _ ->
                val file = File(editText.text.toString())
                when {
                    !file.exists() -> {
                        Toast.makeText(activity, "Directory does not exist", Toast.LENGTH_SHORT).show()
                    }
                    !file.isDirectory -> {
                        Toast.makeText(activity, "Directory is not a folder", Toast.LENGTH_SHORT).show()
                    }
                    !file.canRead() -> {
                        Toast.makeText(activity, "Folder is not readable", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        browserViewModel!!.currPath = file
                        setData(browserViewModel!!.currPath)
                        if (file.parent != null) supportActivity!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        else supportActivity!!.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    }
                }
            }
            setView(editText)
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            show()
        }
    }

    private fun shiftFolderForth(name: String) {
        val file = File(browserViewModel!!.currPath, name)
        if (file.canRead()) {
            browserViewModel!!.currPath = file
            setData(browserViewModel!!.currPath)
            supportActivity!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            browserViewModel!!.isExitable = false
        } else {
            Toast.makeText(activity, "Can not read directory", Toast.LENGTH_SHORT).show()
        }
    }

    fun shiftFolderBack() {
        val nextPath = browserViewModel!!.currPath.parentFile ?: browserViewModel!!.currPath
        if (nextPath.canRead()) {
            setData(nextPath)
            if (browserViewModel!!.currPath.path?.compareTo("/") == 0) {
                supportActivity!!.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        } else {
            if (browserViewModel!!.isExitable) {
                activity!!.finish()
            } else {
                val toast = Toast.makeText(activity, "Can not read parent directory\nPress again to exit", Toast.LENGTH_SHORT)
                val v = toast.view.findViewById(android.R.id.message) as TextView
                v.gravity = Gravity.CENTER
                toast.show()
                browserViewModel!!.isExitable = true
            }
        }
    }

    private fun checkReadPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun askReadPermission() {
        ActivityCompat.requestPermissions(activity!!,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_STORAGE)
    }

    override fun onFolderClicked(folder: File) {
        shiftFolderForth(folder.name)
        listener!!.onFolderClicked(folder)
    }

    override fun onFileClicked(file: File) {
        listener!!.onFileClicked(file)
    }

    override fun onFolderLongClicked(folder: File, viewHolder: RecyclerView.ViewHolder) {
        listener!!.onFolderLongClicked(folder, viewHolder)
    }

    override fun onFileLongClicked(file: File, viewHolder: RecyclerView.ViewHolder) {
        listener!!.onFileLongClicked(file, viewHolder)
    }


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setData(browserViewModel!!.currPath)
                } else {
                    Toast.makeText(activity, getString(R.string.no_permission), Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun setData(newPath: File) {
        if (checkReadPermission()) {
            browserViewModel!!.currPath = newPath
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) askReadPermission()
        }
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val controller =
                AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_fall_down)

        recyclerView.layoutAnimation = controller
        recyclerView.adapter.notifyDataSetChanged()
        recyclerView.scheduleLayoutAnimation()
    }

}
