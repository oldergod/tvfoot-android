package com.benoitquenaudon.tvfoot.red.app.domain.matches.filters


import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.benoitquenaudon.rxdatabinding.databinding.RxObservableBoolean
import com.benoitquenaudon.tvfoot.red.R
import com.benoitquenaudon.tvfoot.red.app.common.BaseFragment
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ClearFilters
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ClearSearchIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.FilterInitialIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.SearchTeamIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.SearchedTeamSelectedIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ToggleFilterIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesViewModel
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesViewState
import com.benoitquenaudon.tvfoot.red.app.domain.matches.filters.FiltersItemDisplayable.FiltersAppliableItem.FiltersCompetitionDisplayable
import com.benoitquenaudon.tvfoot.red.app.mvi.MviView
import com.benoitquenaudon.tvfoot.red.databinding.FragmentFiltersBinding
import com.jakewharton.rxbinding2.support.v7.widget.RxToolbar
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class FiltersFragment : BaseFragment(), MviView<MatchesIntent, MatchesViewState> {
  @Inject lateinit var disposables: CompositeDisposable
  @Inject lateinit var bindingModel: FiltersBindingModel
  @Inject lateinit var filtersAdapter: FiltersAdapter
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val viewModel: MatchesViewModel by lazy(NONE) {
    // we need to use MatchesActivity to get only one instance of the MatchesViewModel
    ViewModelProviders.of(activity!!, viewModelFactory).get(MatchesViewModel::class.java)
  }

  lateinit var binding: FragmentFiltersBinding

  companion object Factory {
    fun newInstance(): FiltersFragment = FiltersFragment()
  }

  @Suppress("OverridingDeprecatedMember", "DEPRECATION")
  override fun onAttach(activity: Activity?) {
    fragmentComponent.inject(this)
    super.onAttach(activity)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_filters, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.filtersToolbar.inflateMenu(R.menu.fragment_filters)

    binding.filtersList.apply {
      addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
      adapter = filtersAdapter
    }

    bind()
  }

  override fun onDestroyView() {
    disposables.dispose()
    super.onDestroyView()
  }

  override fun intents(): Observable<MatchesIntent> {
    return Observable.merge(
        Observable.merge(
            initialIntent(),
            clearFilterIntent(),
            filterClickIntent()
        ),
        Observable.merge(
            searchedTeamSelectedIntent(),
            searchTeamIntent(),
            clearSearchTeamInputIntent()
        )
    )
  }

  private fun initialIntent(): Observable<FilterInitialIntent> =
      Observable.just(FilterInitialIntent)

  private fun clearFilterIntent(): Observable<ClearFilters> =
      RxToolbar
          .itemClicks(binding.filtersToolbar)
          .filter { it.itemId == R.id.action_clear }
          .map { ClearFilters }

  private fun filterClickIntent(): Observable<ToggleFilterIntent> =
      filtersAdapter.filterItemClickObservable
          .ofType(FiltersCompetitionDisplayable::class.java)
          .map { ToggleFilterIntent(it.code) }

  private fun searchTeamIntent(): Observable<SearchTeamIntent> =
      filtersAdapter.filterSearchInputObservable
          .filter { it.length > 2 }
          .debounce(300, MILLISECONDS)
          .map(::SearchTeamIntent)

  private fun clearSearchTeamInputIntent(): Observable<ClearSearchIntent> =
      filtersAdapter.filterSearchInputObservable
          .filter { it.length < 3 }
          .map { ClearSearchIntent }

  private fun searchedTeamSelectedIntent(): Observable<SearchedTeamSelectedIntent> {
    return filtersAdapter.searchedTeamClickObservable
        .map(::SearchedTeamSelectedIntent)
  }

  override fun render(state: MatchesViewState) {
    bindingModel.updateFromState(state)
  }

  private fun bind() {
    disposables.add(viewModel.states().subscribe(this::render))
    viewModel.processIntents(intents())
    disposables.add(
        RxObservableBoolean.propertyChanges(bindingModel.hasFilters)
            .startWith(bindingModel.hasFilters.get()) // fix for rotation
            .subscribe {
              binding.filtersToolbar.menu.findItem(R.id.action_clear).isVisible = it
            }
    )
  }
}
