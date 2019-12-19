/*
 * Copyright (c) 2019 Huami Inc. All Rights Reserved.
 */
package com.example.attest

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import java.util.*

/**
 * Author: liboxin
 * Email: liboxin@worktile.com
 * Date: 2019/11/29
 * Time: 10:46
 * Desc:
 */
class AtEditText : AppCompatEditText {

    private val cursor: Cursor? = Cursor()
    private val content = LinkedList<Triple<Int, Int, Int>>()
    private var change: String = ""
    var callback: Callback? = null
    private var trigger: Boolean = false
    private val watcher: AtTextWatcher = object : AtTextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            callback?.beforeTextChanged(s, start, count, after)
        }

        /**
         * s: 改变后的字符串
         * start: 有变动的字符串的序号
         * before: 被改变的字符串长度，如果是新增则为0。
         * count: 添加的字符串长度，如果是删除则为0。
         */
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            cursor?.apply {
                // 没有多选
                change = s.toString().substring(start, start + count)
                Log.e("xys", "~~~~~~afterTextChanged change -> $change")
                if (change in callback?.aimsArray ?: arrayOf()) {
                    trigger = true
                }
            }
            callback?.onTextChanged(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            Log.d("xys", "afterTextChanged")
            callback?.afterTextChanged(s)
        }
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?)
            : this(context, attrs, R.attr.editTextStyle)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        addTextChangedListener(watcher)
    }

    override fun addTextChangedListener(watcher: TextWatcher?) {
        if (watcher is AtTextWatcher) {
            super.addTextChangedListener(watcher)
        } else {
            throw RuntimeException("use callback -> AtEditText.Callback")
        }
    }

    fun appendAt(atBean: AtBean) {
        cursor?.apply {
            if (!cursor.isSelected) {
                // 没选文字
                append(
                    atBean.atStringWithoutAt,
                    cursor.selStart,
                    cursor.selStart + atBean.atString.length
                )
            } else {
                // 选择了文字
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        cursor?.apply {
            this.selStart = selStart
            this.selEnd = selEnd
            this.text = this@AtEditText.text?.toString() ?: ""
            if (trigger) {
                // 在这里回调是为了将更新后的cursor带入参数
                callback?.onInputCustomCallback(clone(), change)
                trigger = false
            }
        }
    }

    override fun onDetachedFromWindow() {
        removeTextChangedListener(watcher)
        super.onDetachedFromWindow()
    }

    abstract class Callback(val aimsArray: Array<String>) {

        /**
         * 用户输入自定义回调
         */
        abstract fun onInputCustomCallback(cursor: Cursor, aimsString: String?)

        /**
         * 直接暴露给外界[TextWatcher.beforeTextChanged]的回调
         */
        fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        /**
         * 直接暴露给外界[TextWatcher.onTextChanged]的回调
         */
        fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        /**
         * 直接暴露给外界[TextWatcher.afterTextChanged]的回调
         */
        fun afterTextChanged(s: Editable?) {}
    }

    abstract class AtCallback : Callback(arrayOf("@")) {
        override fun onInputCustomCallback(cursor: Cursor, aimsString: String?) {
            onInputAtCallback(cursor)
        }

        /**
         * 用户输入@回调
         */
        abstract fun onInputAtCallback(cursor: Cursor)
    }

    /**
     * 选择范围
     */
    data class Cursor(var selStart: Int = 0, var selEnd: Int = 0, var text: String = "") :
        Cloneable {
        /**
         * 是否是选择了多个文字
         */
        val isSelected: Boolean
            get() = selStart != selEnd

        /**
         * 多选时选中的文字
         */
        val selectedContent: String
            get() {
                return if (isSelected) {
                    text.substring(selStart, selEnd)
                } else {
                    ""
                }
            }
        /**
         * 选中处之前的文字
         */
        val beforeText: String
            get() {
                return if (isSelected) {
                    text.substring(0, selStart)
                } else {
                    text.substring(0, selStart)
                }
            }
        /**
         * 选中处之后的文字
         */
        val endText: String
            get() {
                return if (isSelected) {
                    text.substring(selEnd, text.length)
                } else {
                    text.substring(selEnd, text.length)
                }
            }
        val isEnd: Boolean
            get() = endText.isEmpty()

        val isStart: Boolean
            get() = beforeText.isEmpty()

        public override fun clone(): Cursor {
            val c = super.clone() as Cursor
            c.selStart = this.selStart
            c.selEnd = this.selEnd
            c.text = this.text
            return c
        }
    }

    /**
     * AT的人
     */
    data class AtBean(
        val name: String,
        val id: String
    ) : Parcelable {
        /**
         * "@"后的字符串
         */
        val atStringWithoutAt: String
            get() = "$name "
        /**
         * 完整的@字符串
         */
        val atString: String
            get() = "@$atStringWithoutAt"


        /**
         * 从此处以下为序列化
         */
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(id)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<AtBean> {
            override fun createFromParcel(parcel: Parcel): AtBean {
                return AtBean(parcel)
            }

            override fun newArray(size: Int): Array<AtBean?> {
                return arrayOfNulls(size)
            }
        }
    }

    private interface AtTextWatcher : TextWatcher
}

