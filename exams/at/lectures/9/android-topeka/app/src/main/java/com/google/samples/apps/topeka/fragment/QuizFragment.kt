/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.topeka.fragment

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.topeka.R
import com.google.samples.apps.topeka.adapter.QuizAdapter
import com.google.samples.apps.topeka.adapter.ScoreAdapter
import com.google.samples.apps.topeka.helper.*
import com.google.samples.apps.topeka.model.Category
import com.google.samples.apps.topeka.widget.quiz.AbsQuizView
import kotlinx.android.synthetic.main.fragment_quiz.*

/**
 * Encapsulates Quiz solving and displays it to the user.
 */
class QuizFragment : Fragment() {

  private val category by lazy(LazyThreadSafetyMode.NONE) {
    val categoryId = arguments!!.getString(Category.TAG)
    activity!!.database().getCategoryWith(categoryId!!)
  }

  private val quizAdapter by lazy(LazyThreadSafetyMode.NONE) {
    context?.run {
      QuizAdapter(this, category)
    }
  }

  val scoreAdapter by lazy(LazyThreadSafetyMode.NONE) {
    context?.run {
      ScoreAdapter(this, category)
    }
  }

  private var solvedStateListener: SolvedStateListener? = null

  override fun onCreateView(inflater: LayoutInflater,
                            container: ViewGroup?,
                            savedInstanceState: Bundle?): View {
    // Create a themed Context and custom LayoutInflater
    // to get custom themed views in this Fragment.
    val context = ContextThemeWrapper(activity, category.theme.styleId)
    return context.inflate(R.layout.fragment_quiz, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setProgress(category.firstUnsolvedQuizPosition)
    decideOnViewToDisplay()
    setQuizViewAnimations()
    setAvatarDrawable()
    super.onViewCreated(view, savedInstanceState)
  }

  private fun decideOnViewToDisplay() {
    if (category.solved) {
      // display the summary
      with(scorecard) {
        adapter = scoreAdapter
        visibility = View.VISIBLE
      }
      quiz_view.visibility = View.GONE
      solvedStateListener?.onCategorySolved()
    } else {
      with(quiz_view) {
        adapter = quizAdapter
        setSelection(category.firstUnsolvedQuizPosition)
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private fun setQuizViewAnimations() {
    if (ApiLevelHelper.isLowerThan(Build.VERSION_CODES.LOLLIPOP)) {
      return
    }
    with(quiz_view) {
      setInAnimation(activity, R.animator.slide_in_bottom)
      setOutAnimation(activity, R.animator.slide_out_top)
    }
  }

  private fun setAvatarDrawable() {
    activity?.requestLogin { player ->
      if (player.valid()) {
        avatar.setAvatar(player.avatar!!.drawableId)
        with(ViewCompat.animate(avatar)) {
          interpolator = FastOutLinearInInterpolator()
          startDelay = 500
          scaleX(1f)
          scaleY(1f)
          start()
        }
      }
    }
  }

  private fun setProgress(currentQuizPosition: Int) {
    if (isAdded) {
      progress_text.text = getString(R.string.quiz_of_quizzes, currentQuizPosition,
          category.quizzes.size)
      progress.progress = currentQuizPosition
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    val focusedChild = quiz_view.focusedChild
    if (focusedChild is ViewGroup) {
      val currentView = focusedChild.getChildAt(0)
      if (currentView is AbsQuizView<*>) {
        outState.putBundle(KEY_USER_INPUT, currentView.userInput)
      }
    }
    super.onSaveInstanceState(outState)
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    restoreQuizState(savedInstanceState)
    super.onViewStateRestored(savedInstanceState)
  }

  private fun restoreQuizState(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) return
    quiz_view.onLayoutChange {
      val currentChild = (this as ViewGroup).getChildAt(0)
      if (currentChild is ViewGroup) {
        val potentialQuizView = currentChild.getChildAt(0)
        if (potentialQuizView is AbsQuizView<*>) {
          potentialQuizView.userInput = savedInstanceState.getBundle(KEY_USER_INPUT)
              ?: Bundle.EMPTY
        }
      }
    }

  }

  /**
   * Displays the next page.

   * @return `true` if there's another quiz to solve, else `false`.
   */
  fun showNextPage(): Boolean {
    quiz_view.let {
      val nextItem = it.displayedChild + 1
      setProgress(nextItem)
      if (nextItem < it.adapter.count) {
        it.showNext()
        activity?.database()?.updateCategory(category)
        return true
      }
    }
    markCategorySolved()
    return false
  }

  private fun markCategorySolved() {
    category.solved = true
    activity?.database()?.updateCategory(category)
  }

  fun hasSolvedStateListener() = solvedStateListener != null

  fun setSolvedStateListener(solvedStateListener: SolvedStateListener) {
    this.solvedStateListener = solvedStateListener
    if (category.solved) solvedStateListener.onCategorySolved()
  }

  /**
   * Interface definition for a callback to be invoked when the quiz is started.
   */
  interface SolvedStateListener {

    /**
     * This method will be invoked when the category has been solved.
     */
    fun onCategorySolved()
  }

  companion object {

    private const val KEY_USER_INPUT = "USER_INPUT"

    fun newInstance(categoryId: String,
                    listener: SolvedStateListener?
    ): QuizFragment {
      return QuizFragment().apply {
        solvedStateListener = listener
        arguments = Bundle().apply { putString(Category.TAG, categoryId) }
      }
    }
  }
}