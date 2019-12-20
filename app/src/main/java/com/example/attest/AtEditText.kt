/*
 * Copyright (c) 2019 Huami Inc. All Rights Reserved.
 */
package com.example.attest

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import java.lang.ref.WeakReference
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
    private val content = LinkedList<ContentBean>()
    private var change: String = ""
    var callback: Callback? = null
    /**
     * 标志位，满足[Callback.aimsArray]触发
     */
    private var trigger: Boolean = false
    private val watcher: AtTextWatcher = object : AtTextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            callback?.beforeTextChanged(s, start, count, after)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            cursor?.apply {
                // 没有多选
                change = s.toString().substring(start, start + count)
                if (change in callback?.aimsArray ?: AIMS_EMPTY_ARRAY) {
                    trigger = true
                }
            }
            callback?.onTextChanged(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            callback?.afterTextChanged(s)
            if (trigger) {
                callback?.onInputCustomCallback(change)
            }
            trigger = false
        }
    }
    private val cursorHandler: CursorHandler = CursorHandler(this)

    companion object {
        private val AIMS_EMPTY_ARRAY: Array<String> = arrayOf()
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

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        Log.d("xys", "onSelectionChanged")
        cursor?.apply {
            this.selStart = selStart
            this.selEnd = selEnd
            this.text = this@AtEditText.text?.toString() ?: ""
            cursorHandler.cursorChange()
        }
    }

    fun appendAt(atBean: AtBean) {
        cursor?.apply {
            // 如果在已存在@前插入新@，那么后面的@对应pos都要向后移动atString的长度
            content.filter { it.startPos >= selStart }.forEach {
                it.startPos += atBean.atString.length
            }
            // 添加新的@信息到内存
            content.add(AtContent(cursor.selStart, atBean))
            // 并写入view
            editableText.insert(selStart, atBean.atString)
        }
    }

    /**
     * 回退
     */
    fun backSpace(length: Int = 1) {
        if (length < 0) {
            throw  RuntimeException("length must be >= 0")
        }
        cursor?.apply {
            editableText.delete(0.coerceAtLeast(selStart - length), selStart)
        }
    }

    private fun getCursorInContentBean(): ContentBean? {
        cursor?.apply {
            content.forEach { bean ->
                if (selStart in (bean.startPos + 1)..bean.endPos) {
                    return bean
                }
            }
        }
        return null
    }


    override fun onDetachedFromWindow() {
        removeTextChangedListener(watcher)
        cursorHandler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    abstract class Callback(val aimsArray: Array<String>) {

        /**
         * 用户输入自定义回调
         */
        abstract fun onInputCustomCallback(aimsString: String?)

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
        override fun onInputCustomCallback(aimsString: String?) {
            onInputAtCallback()
        }

        /**
         * 用户输入@回调
         */
        abstract fun onInputAtCallback()
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
    data class AtBean(val name: CharSequence, val id: String) : Parcelable {
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
            parcel.writeString(name.toString())
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

    private abstract class ContentBean(var startPos: Int) {
        open var content: String? = null

        open val endPos: Int = 0
    }

    private class AtContent(start: Int, val atBean: AtBean) : ContentBean(start) {
        override var content: String? = ""
            get() = atBean.atString
            set(value) {
                field = value
            }
        override val endPos: Int
            get() = startPos.plus(atBean.atString.length) - 1
    }

    private class NormalContent(start: Int, val text: String) : ContentBean(start) {
        override var content: String? = null
            get() = text
            set(value) {
                field = value
            }
        override val endPos: Int
            get() = startPos.plus(text.length)
    }

    private class CursorHandler(atEditText: AtEditText) : Handler() {
        private val catch = WeakReference<AtEditText>(atEditText)

        companion object {
            private const val CURSOR_CHANGE = 0x010
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                CURSOR_CHANGE -> {
                    // 不允许光标出现在@好友中间
                    catch.get()?.apply {
                        getCursorInContentBean()?.apply {
                            setSelection(endPos + 1)
                        }
                    }
                }
                else -> {
                }
            }
        }

        fun cursorChange(delayed: Long = 300) {
            removeMessages(CURSOR_CHANGE)
            sendMessageDelayed(Message.obtain(this, CURSOR_CHANGE), delayed)
        }
    }
}