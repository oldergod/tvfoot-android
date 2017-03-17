package io.oldering.tvfoot.red.matches;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MatchesBinder {
  private final MatchesActivity activity;
  private final MatchesRepository repository;

  public MatchesBinder(MatchesActivity activity, MatchesRepository repository) {
    this.activity = activity;
    this.repository = repository;
  }

  private Observable<MatchesIntent> intent() {
    return Observable.merge(activity.loadFirstPageIntent(), activity.loadNextPageIntent(),
        activity.matchRowClickIntent()).subscribeOn(AndroidSchedulers.mainThread());
  }

  private Observable<MatchesViewState> model(Observable<MatchesIntent> intents) {
    return intents.flatMap(intent -> {
      if (intent instanceof MatchesIntent.LoadFirstPage) {
        return repository.loadFirstPage().subscribeOn(Schedulers.io());
      }
      if (intent instanceof MatchesIntent.LoadNextPage) {
        return repository.loadNextPage().subscribeOn(Schedulers.io());
      }
      if (intent instanceof MatchesIntent.MatchRowClick) {
        return Observable.just(
            MatchesViewState.matchRowClick(((MatchesIntent.MatchRowClick) intent).getMatch()));
      }
      throw new IllegalArgumentException("I don't know how to deal with this intent " + intent);
    }).scan(MatchesViewState::reduce).subscribeOn(Schedulers.io());
  }

  private void view(Observable<MatchesViewState> states) {
    states.observeOn(AndroidSchedulers.mainThread()).subscribe(activity::render);
  }

  void bind() {
    view(model(intent()));
  }
}