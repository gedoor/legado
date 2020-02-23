package io.legado.app.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun <T : ViewModel> AppCompatActivity.getViewModel(clazz: Class<T>) =
    ViewModelProvider(this).get(clazz)

fun <T : ViewModel> Fragment.getViewModel(clazz: Class<T>) =
    ViewModelProvider(this).get(clazz)

/**
 * 与activity数据同步
 */
fun <T : ViewModel> Fragment.getViewModelOfActivity(clazz: Class<T>) =
    ViewModelProvider(requireActivity()).get(clazz)