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
import android.view.KeyEvent
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
    val atList = LinkedList<AtContent>()
    private var change: String = ""
    var callback: Callback? = null
    /**
     * 标志位，满足[Callback.aimsArray]触发
     */
    private var trigger: Boolean = false
    private var fromUser: Boolean = true
    private val watcher: AtTextWatcher = object : AtTextWatcher {
        /**
         * start: 字符串中即将发生修改的位置。
         * count: 字符串中即将被修改的文字的长度。如果是新增的话则为0。
         * after: 被修改的文字修改之后的长度。如果是删除的话则为0。
         */
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (fromUser) {
                cursor?.apply {
                    // 将文字后的AtBean后移
                    if (s.toString().isNotEmpty()) {
                        atList.filter {
                            Log.d("xys", "${it.content}  ${it.startPos}  ${it.endPos}  $selStart")
                            it.startPos >= selStart
                        }.forEach {
                            Log.d("xys", "after = ${after} ")
                            it.startPos += after
                        }
                    }
                }
            }
            callback?.beforeTextChanged(s, start, count, after)
        }

        /**
         * start: 有变动的字符串的序号
         * before: 被改变的字符串长度，如果是新增则为0。
         * count: 添加的字符串长度，如果是删除则为0。
         */
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            cursor?.apply {
                // 增加文字时，当前变化的字符串，删除时为""
                change = s.toString().substring(start, start + count)
                if (change in callback?.aimsArray ?: AIMS_EMPTY_ARRAY) {
                    trigger = true
                }
            }
            callback?.onTextChanged(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            if (trigger) {
                callback?.onInputCustomCallback(change)
            }
            trigger = false
            callback?.afterTextChanged(s)
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
        cursor?.apply {
            this.selStart = selStart
            this.selEnd = selEnd
            this.text = this@AtEditText.text?.toString() ?: ""
            cursorHandler.cursorChange()
        }
    }

    fun appendAt(atBean: AtBean) {
        cursor?.apply {
            if (AtContent(selStart, atBean) in atList) {
                val sameAllow = callback?.appendSameAllow(atBean) ?: true
                if (!sameAllow) {
                    return
                }
            }
            // 如果在已存在@前插入新@，那么后面的@对应pos都要向后移动atString的长度
            atList.filter { it.startPos >= selStart }.forEach {
                it.startPos += atBean.atString.length
            }
            // 添加新的@信息到内存
            atList.add(AtContent(cursor.selStart, atBean))
            // 并写入view
            editableTextInsert(selStart, atBean.atString)
        }
    }

    /**
     * 回退
     */
    fun backSpace() {
        val bean = getCursorInContentBean() ?: getCursorBeforeContentBean()
        if (bean == null) {
            // 正常删除文字
            cursor?.apply {
                // 将删除文字后的每个@好友位置-1
                atList.filter { it.startPos >= selStart }.forEach {
                    it.startPos -= 1
                }
                editableTextDelete(0.coerceAtLeast(selStart - 1), selStart)
            }
        } else {
            cursor?.apply {
                // 将删除文字后的每个@好友位置向前移
                atList.filter { it.startPos >= selStart }.forEach {
                    it.startPos -= it.content?.length ?: 0
                }
                // 删除一个@好友
                editableTextDelete(bean.startPos, bean.endPos + 1)
                atList.remove(bean)
            }
        }
    }

    private fun editableTextDelete(st: Int, en: Int) {
        fromUser = false
        editableText.delete(st, en)
        fromUser = true
    }

    private fun editableTextInsert(where: Int, text: CharSequence) {
        fromUser = false
        editableText.insert(where, text)
        fromUser = true
    }

    /**
     * Cursor是否在@还有字段当中
     */
    private fun getCursorInContentBean(): AtContent? {
        cursor?.apply {
            atList.forEach {
                val range = (it.startPos + 1)..it.endPos
                if (selStart in range || selEnd in range) {
                    return it
                }
            }
        }
        return null
    }

    /**
     * Cursor的前一个字符是否是@好友
     */
    private fun getCursorBeforeContentBean(): AtContent? {
        cursor?.apply {
            atList.forEach {
                if (it.endPos + 1 == selStart) {
                    return it
                }
            }
        }
        return null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            backSpace()
            return false
        }
        return super.onKeyDown(keyCode, event)
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

        /**
         * 是否允许相同的AtBean
         */
        abstract fun appendSameAllow(atBean: AtBean): Boolean
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AtBean

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

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

    class AtContent(var startPos: Int, val atBean: AtBean) {
        var content: String? = ""
            get() = atBean.atString
            set(value) {
                field = value
            }
        val endPos: Int
            get() = startPos.plus(atBean.atString.length) - 1

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AtContent

            if (atBean != other.atBean) return false

            return true
        }

        override fun hashCode(): Int {
            return atBean.hashCode()
        }
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
                            try {
                                setSelection(endPos + 1)
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }
        }

        fun cursorChange(delayed: Long = 300) {
            removeMessages(CURSOR_CHANGE)
            sendMessageDelayed(Message.obtain(this, CURSOR_CHANGE), delayed)
        }
    }
}