package com.example.attest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_user.*
import kotlinx.android.synthetic.main.item_user.view.*

class UserActivity : AppCompatActivity() {

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, UserActivity::class.java)
            activity.startActivityForResult(intent, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        rv_user.layoutManager = LinearLayoutManager(this)
        rv_user.adapter = MyAdapter(this, object : MyAdapter.OnClickListener {
            override fun onClick(user: AtEditText.AtBean) {
                val intent = Intent()
                intent.putExtra("user", user)
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    private class MyAdapter(
        val context: Context,
        val listener: OnClickListener
    ) : RecyclerView.Adapter<ViewHolder>() {

        private val user: List<AtEditText.AtBean> = List(20) {
            AtEditText.AtBean("张$it", it.toString())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_user, parent, false))

        override fun getItemCount(): Int = user.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.apply {
                val user = user[position]
                tv_user.text = user.name
                setOnClickListener {
                    listener.onClick(user)
                }
            }
        }

        interface OnClickListener {
            fun onClick(user: AtEditText.AtBean)
        }
    }

    private class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
