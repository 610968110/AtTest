package com.example.attest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et.callback = object : AtEditText.AtCallback() {
            override fun onInputAtCallback() {
                UserActivity.start(this@MainActivity)
            }

            override fun appendSameAllow(atBean: AtEditText.AtBean): Boolean {
                return true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getParcelableExtra<AtEditText.AtBean>("user")?.apply {
                    Log.e("xys", "onActivityResult:$this")
                    et.backSpace()
                    et.appendAt(this)
                }
            }
        }
    }

    fun seeList(view: View) {
        et.atList.forEach {
            Log.w("xys", "${it.content}  ${it.startPos}  ${it.endPos}")
        }
        Log.w("xys", "````````````````````````````")
    }
}
