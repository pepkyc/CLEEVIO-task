package com.pepkyc.cleevio.fragment

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.adapters.FileAdapter
import com.pepkyc.cleevio.listeners.ActionModeCallbacksListener
import com.pepkyc.cleevio.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.activity_file_browser.*
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FileBrowserFragmentT.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FileBrowserFragmentT.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FileBrowserFragmentT : Fragment(), FileAdapter.BrowseInteractionListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val PERMISSIONS_REQUEST_READ_STORAGE = 24

    var browserViewModel: BrowserViewModel? = null
    private var sharedPref: SharedPreferences? = null
    var adapter: FileAdapter? = null
    var actionModeCallback: ActionModeCallbacksListener? = null
    private val supportActivity = activity as AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_browser_fragment_t, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        browserViewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)

        if (browserViewModel!!.currPath.parent != null) supportActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
                supportActivity.startSupportActionMode(it)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onResume() {
        super.onResume()
        setData(browserViewModel!!.currPath)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                FileBrowserFragmentT().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
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
                        if (file.parent != null) supportActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        else supportActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
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
            supportActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            browserViewModel!!.isExitable = false
        } else {
            Toast.makeText(activity, "Can not read directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shiftFolderBack() {
        val nextPath = browserViewModel!!.currPath.parentFile ?: browserViewModel!!.currPath
        if (nextPath.canRead()) {
            browserViewModel!!.currPath = nextPath
            setData(browserViewModel!!.currPath)
            if (browserViewModel!!.currPath.path?.compareTo("/") == 0) {
                supportActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
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
//        openInDefaultAcivity(file)
    }


//    override fun onBackPressed() {
//        if (browserViewModel!!.currPath?.path?.compareTo("/") == 0) {
//            super.onBackPressed()
//        } else {
//            shiftFolderBack()
//        }
//    }

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
