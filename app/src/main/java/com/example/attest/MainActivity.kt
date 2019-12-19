package com.example.attest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
}
