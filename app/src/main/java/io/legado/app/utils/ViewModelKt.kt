package io.legado.app.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders

fun <T : ViewModel> AppCompatActivity.getViewModel(clazz: Class<T>) = ViewModelProviders.of(this).get(clazz)

fun <T : ViewModel> Fragment.getViewModel(clazz: Class<T>) = ViewModelProviders.of(this).get(clazz)

fun <T : ViewModel> Fragment.getViewModelOfActivity(clazz: Class<T>) = ViewModelProviders.of(requireActivity()).get(clazz)
