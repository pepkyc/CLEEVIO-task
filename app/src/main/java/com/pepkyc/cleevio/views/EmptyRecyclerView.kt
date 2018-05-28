package com.pepkyc.cleevio.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View


class EmptyRecyclerView : RecyclerView {
    private var emptyView: View? = null

    private val emptyObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            val adapter = adapter
            if (adapter != null && emptyView != null) {
                if (adapter.itemCount == 0) {
                    emptyView?.visibility = View.VISIBLE
                    this@EmptyRecyclerView.visibility = View.GONE
                } else {
                    emptyView?.visibility = View.GONE
                    this@EmptyRecyclerView.visibility = View.VISIBLE
                }
            }

        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle : Int) : super(context, attrs, defStyle)

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        super.setAdapter(adapter)

        if(!adapter?.hasObservers()!!) adapter.registerAdapterDataObserver(emptyObserver)

        emptyObserver.onChanged()
    }

    fun setEmptyView(emptyView: View) {
        this.emptyView = emptyView
    }
}