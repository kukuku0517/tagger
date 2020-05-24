package com.project.tagger.util.widget

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup.LayoutParams
import android.widget.ProgressBar
import com.project.tagger.R


class TransparentDialog {
    interface OnTransparentDialogListener{
        fun onCancel()
    }


    companion object{
        const val TIME_OUT = 101
        private var mLoadingDialog: Dialog? = null

        @JvmStatic
        val isLoading: Boolean
            get() = mLoadingDialog != null

        private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: Message) {
                if (msg.what == TIME_OUT) {
                    if (mLoadingDialog != null) {
                        try {
                            mLoadingDialog!!.dismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        }

        @JvmStatic
        fun showLoading(context: Context) {
            if (mLoadingDialog == null) {
                mLoadingDialog = Dialog(context, R.style.TransDialog)
                val pb = ProgressBar(context)
                val params = LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT)
                mLoadingDialog!!.addContentView(pb, params)
                mLoadingDialog!!.setCancelable(false)
            }

            try {
                mLoadingDialog!!.show()
                mHandler.sendEmptyMessageDelayed(TIME_OUT, 7000)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        @JvmStatic
        fun showLoading(context: Context, timeOut: Int) {
            if (mLoadingDialog == null) {
                mLoadingDialog = Dialog(context, R.style.TransDialog)
                val pb = ProgressBar(context)
                val params = LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT)
                mLoadingDialog!!.addContentView(pb, params)
                mLoadingDialog!!.setCancelable(false)
            }

            try {
                mLoadingDialog!!.show()
                mHandler.sendEmptyMessageDelayed(TIME_OUT, timeOut.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        @JvmStatic
        fun showLoading(context: Context, timeOut: Int, cancelable: Boolean) {
            if (mLoadingDialog == null) {
                mLoadingDialog = Dialog(context, R.style.TransDialog)
                val pb = ProgressBar(context)
                val params = LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT)
                mLoadingDialog!!.addContentView(pb, params)
                mLoadingDialog!!.setCancelable(cancelable)
            }

            try {
                mLoadingDialog!!.show()
                if (timeOut >= 0) {
                    mHandler.sendEmptyMessageDelayed(TIME_OUT, timeOut.toLong())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        @JvmStatic
        fun showLoading(context: Context, cancelCallback:OnTransparentDialogListener) {
            if (mLoadingDialog == null) {
                mLoadingDialog = Dialog(context, R.style.TransDialog)
                val pb = ProgressBar(context)
                val params = LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT)
                mLoadingDialog!!.addContentView(pb, params)
                mLoadingDialog!!.setCancelable(true)
                mLoadingDialog!!.setCanceledOnTouchOutside(false)

            }

            try {
                mLoadingDialog!!.show()
                mHandler.sendEmptyMessageDelayed(TIME_OUT, 60000)
                mLoadingDialog?.setOnCancelListener {
                    cancelCallback.onCancel()
                    it.dismiss()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



        @JvmStatic
        fun hideLoading() {
            try {
                if (mLoadingDialog != null) {
                    mLoadingDialog!!.dismiss()
                    mLoadingDialog = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}