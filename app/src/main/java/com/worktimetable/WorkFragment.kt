package com.worktimetable

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.worktimetable.databinding.FragmentWorkBinding



class WorkFragment : Fragment() {


    companion object { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentWorkBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _vBinding = FragmentWorkBinding.inflate(inflater, container, false)
        return vBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MainActivity){
            mainActivity = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vBinding.mkWorkTypeBtn.setOnClickListener {
            mkWorkDilog()
        }
    }

    private fun mkWorkDilog(){
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_work)
            val layoutParams = window?.attributes
            val viewWidth = vBinding.workDialogLayout.width
            val viewHeight = vBinding.workDialogLayout.height
            layoutParams?.width = (viewWidth * 0.95).toInt()
            layoutParams?.height = (viewHeight * 0.9).toInt()
            window?.attributes = layoutParams

            this.findViewById<Button>(R.id.mkAddWorkItemDialog).setOnClickListener {

            }


            show()
        }
    }


}

/*private fun mkSubWorkerDialog(callback: (ArrayList<String>) -> Unit){
		val resultArrayList = arrayListOf<String>()
		val inflater = LayoutInflater.from(this@MainActivity)

		Dialog(this@MainActivity).apply {
			this.setContentView(R.layout.dialog_sub_worker)
			val subWorkerLayout = findViewById<LinearLayout>(R.id.subWorkerLayout)
			subWorkerList.forEach { subName->
				resultArrayList.add(subName)
				val holder = inflater.inflate(R.layout.holder_sub_worker, null) as LinearLayout
				val holderTV = holder.findViewById<TextView>(R.id.subWorkerTV)
				holderTV.text = subName
				subWorkerLayout.addView(holder)
				holder.findViewById<ImageButton>(R.id.subWorkerDelBtn).setOnClickListener{
					subWorkerLayout.removeView(holder)
					resultArrayList.remove(holder.findViewById<TextView>(R.id.subWorkerTV).text.toString())
				}
			}

			this.findViewById<ImageButton>(R.id.addSubWorkerBtn).setOnClickListener {
				val subET = findViewById<EditText>(R.id.subWorkerET)
				val holder = inflater.inflate(R.layout.holder_sub_worker, null) as LinearLayout
				val holderTV = holder.findViewById<TextView>(R.id.subWorkerTV)
				holderTV.text = subET.text
				resultArrayList.add(subET.text.toString())
				holder.findViewById<ImageButton>(R.id.subWorkerDelBtn).setOnClickListener{
					subWorkerLayout.removeView(holder)
					resultArrayList.remove(holder.findViewById<TextView>(R.id.subWorkerTV).text.toString())
				}
				subET.setText("")
				subWorkerLayout.addView(holder)
			}

			this.findViewById<Button>(R.id.setSubWorkerBtn).setOnClickListener {
				this.dismiss()
				callback(resultArrayList)
			}
			this.show()
			window?.attributes.apply {
				this?.width=(vBinding.mainLayout.width * 0.9).toInt()
				window?.attributes = this
			}
		}
	}private fun mkSubWorkerDialog(callback: (ArrayList<String>) -> Unit){
		val resultArrayList = arrayListOf<String>()
		val inflater = LayoutInflater.from(this@MainActivity)

		Dialog(this@MainActivity).apply {
			this.setContentView(R.layout.dialog_sub_worker)
			val subWorkerLayout = findViewById<LinearLayout>(R.id.subWorkerLayout)
			subWorkerList.forEach { subName->
				resultArrayList.add(subName)
				val holder = inflater.inflate(R.layout.holder_sub_worker, null) as LinearLayout
				val holderTV = holder.findViewById<TextView>(R.id.subWorkerTV)
				holderTV.text = subName
				subWorkerLayout.addView(holder)
				holder.findViewById<ImageButton>(R.id.subWorkerDelBtn).setOnClickListener{
					subWorkerLayout.removeView(holder)
					resultArrayList.remove(holder.findViewById<TextView>(R.id.subWorkerTV).text.toString())
				}
			}

			this.findViewById<ImageButton>(R.id.addSubWorkerBtn).setOnClickListener {
				val subET = findViewById<EditText>(R.id.subWorkerET)
				val holder = inflater.inflate(R.layout.holder_sub_worker, null) as LinearLayout
				val holderTV = holder.findViewById<TextView>(R.id.subWorkerTV)
				holderTV.text = subET.text
				resultArrayList.add(subET.text.toString())
				holder.findViewById<ImageButton>(R.id.subWorkerDelBtn).setOnClickListener{
					subWorkerLayout.removeView(holder)
					resultArrayList.remove(holder.findViewById<TextView>(R.id.subWorkerTV).text.toString())
				}
				subET.setText("")
				subWorkerLayout.addView(holder)
			}

			this.findViewById<Button>(R.id.setSubWorkerBtn).setOnClickListener {
				this.dismiss()
				callback(resultArrayList)
			}
			this.show()
			window?.attributes.apply {
				this?.width=(vBinding.mainLayout.width * 0.9).toInt()
				window?.attributes = this
			}
		}
	}*/